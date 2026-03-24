package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/**
 * Uses reflection to invoke its members in order to let the JVM sort out the types, which types we don't know.
 */
private abstract class Adapter<T> {
    abstract fun implTo(obj: T): SExpr
    abstract fun implFrom(expr: SExpr): T

    private fun fn(name: String) = javaClass.methods.find { it.name == name }
        ?: error("Internal error: missing $name")

    private val fnTo = fn("implTo")
    private val fnFrom = fn("implFrom")
    fun toSExpr(obj: Any?): SExpr = fnTo(this, obj) as SExpr
    fun fromSExpr(expr: SExpr): Any? = fnFrom(this, expr)
}

/**
 * Exposes initialization to clients.
 */
class SerdeRegistry internal constructor(val serdes: Serdes) {
    /**
     * Create an adapter for a data class.
     * This data class may now appear in supported collections and as members in other data classes.
     */
    inline fun <reified T : Any> register() = serdes.register<T>(typeOf<T>())
}

/**
 * A collection of adapters for translating objects to and from s-expressions.
 * Create an instance, register data classes, invoke toSExpr and fromSExpr.
 * Primitives, (registered) data classes, and pairs, maps, sets, and lists thereof are handled.
 */
class Serdes private constructor() {

    private val adapters = mutableMapOf<Class<*>, Adapter<*>>(
        // Primitive adapters.
        // Collection types are built on the fly, below.
        Byte::class.java to object : Adapter<Byte>() {
            override fun implTo(obj: Byte): SExpr = SAtom(ByteArray(1).also { it[0] = obj })
            override fun implFrom(expr: SExpr): Byte = expr.requireAtom().data[0]
        },
        Char::class.java to object : Adapter<Char>() {
            override fun implTo(obj: Char): SExpr = SAtom(ByteArray(1).also { it[0] = obj.code.toByte() })
            override fun implFrom(expr: SExpr): Char = expr.requireAtom().data[0].toInt().toChar()
        },
        String::class.java to object : Adapter<String>() {
            override fun implTo(obj: String): SExpr = SAtom(obj.toByteArray())
            override fun implFrom(expr: SExpr): String = expr.requireAtom().asString()
        },
        Boolean::class.java to object : Adapter<Boolean>() {
            override fun implTo(obj: Boolean): SExpr = SAtom(obj.toString().toByteArray())
            override fun implFrom(expr: SExpr): Boolean =
                expr.requireAtom().asString().run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        },
        Int::class.java to object : Adapter<Int>() {
            override fun implTo(obj: Int): SExpr = SAtom(obj.toString().toByteArray())
            override fun implFrom(expr: SExpr): Int =
                expr.requireAtom().asString().run { toIntOrNull() ?: error("Expected Int, got $this.") }
        },
        Long::class.java to object : Adapter<Long>() {
            override fun implTo(obj: Long): SExpr = SAtom(obj.toString().toByteArray())
            override fun implFrom(expr: SExpr): Long =
                expr.requireAtom().asString().run { toLongOrNull() ?: error("Expected Long, got $this") }
        },
        Double::class.java to object : Adapter<Double>() {
            override fun implTo(obj: Double): SExpr = SAtom(obj.toString().toByteArray())
            override fun implFrom(expr: SExpr): Double =
                expr.requireAtom().asString().run { toDoubleOrNull() ?: error("Expected Double, got $this") }
        }
    )

    // Find an adapter for this type or die in the attempt.
    // This includes building adapters for parameterized types using their (reified) type arguments.
    private fun adapterFor(kType: KType): Adapter<*> {
        val kClass = kType.classifier as KClass<*>
        return adapters[kClass.java] ?: when (kClass) {
            List::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<List<*>>() {
                    override fun implTo(obj: List<*>): SExpr = fromList(itemType, obj)
                    override fun implFrom(expr: SExpr): List<*> = toList(itemType, expr.requireList())
                }
            }

            Set::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<Set<*>>() {
                    override fun implTo(obj: Set<*>): SExpr = fromSet(itemType, obj)
                    override fun implFrom(expr: SExpr): Set<*> = toSet(itemType, expr.requireList())
                }
            }

            Pair::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Pair<*, *>>() {
                    override fun implTo(obj: Pair<*, *>): SExpr = fromPair(type1, type2, obj)
                    override fun implFrom(expr: SExpr): Pair<*, *> = toPair(type1, type2, expr.requireList())
                }
            }

            Map::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Map<*, *>>() {
                    override fun implTo(obj: Map<*, *>): SExpr = fromMap(type1, type2, obj)
                    override fun implFrom(expr: SExpr): Map<*, *> = toMap(type1, type2, expr.requireList())
                }
            }

            else -> error("Unsupported adapter type $kClass")
        }
    }

    @PublishedApi
    internal fun <T : Any> register(kType: KType) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        require(kClass.isData) { "${kClass.qualifiedName} is not a data class" }
        val ctor = kClass.primaryConstructor ?: error("No primary constructor found for ${kClass.qualifiedName}")
        val adaptersByName: Map<String, Adapter<*>> = buildMap {
            ctor.parameters.forEach { param ->
                require(!param.isVararg) { "Varargs not supported." }
                put(param.name!!, adapterFor(param.type))
            }
        }
        val paramsByName = ctor.parameters.associateBy { it.name }
        adapters[kClass.java] = object : Adapter<T>() {
            override fun implTo(obj: T): SExpr = buildSExpr {
                adaptersByName.forEach { (name, adapter) ->
                    list {
                        atom(name.toByteArray(Charsets.UTF_8))
                        val property = kClass.memberProperties.firstOrNull { it.name == name }
                            ?: error("${kClass.qualifiedName} has no property with name '$name'")
                        val value = property.get(obj)
                        val expr = adapter.toSExpr(value)
                        any(expr)
                    }
                }
            }

            override fun implFrom(expr: SExpr): T = expr.requireList().run {
                ctor.callBy(buildMap {
                    exprs.forEach { expr ->
                        val list = expr.requireList().exprs
                        require(2 == list.size) { "Malformed value entry: ${list.size}" }
                        val name = list[0].requireAtom().asString()
                        val param = paramsByName[name] ?: error("Unknown param $name")
                        val adapter = adaptersByName[name] ?: error("Unknown param $name")
                        val s = adapter.fromSExpr(list[1])
                        put(param, s)
                    }
                })
            }
        }
    }

    /**
     * Render an object as an s-expression.
     */
    inline fun <reified T> toSExpr(obj: T): SExpr = toSExpr(typeOf<T>(), obj)

    @PublishedApi
    internal fun <T> toSExpr(kType: KType, obj: T): SExpr = adapterFor(kType).toSExpr(obj)

    /**
     * Create an object from an s-expression.
     */
    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T = adapterFor(kType).fromSExpr(expr) as T

    // Collection handlers.

    private fun fromList(itemType: KType, obj: List<*>): SList {
        val adapter = adapterFor(itemType)
        return buildSExpr {
            obj.forEach { any(adapter.toSExpr(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toList(itemType: KType, expr: SList): T = buildList {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.fromSExpr(it)) }
    } as T

    private fun fromSet(itemType: KType, obj: Set<*>): SList {
        val adapter = adapterFor(itemType)
        return buildSExpr {
            obj.forEach { any(adapter.toSExpr(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toSet(itemType: KType, expr: SList): T = buildSet {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.fromSExpr(it)) }
    } as T

    private fun fromPair(type1: KType, type2: KType, obj: Pair<*, *>): SList = buildSExpr {
        any(adapterFor(type1).toSExpr(obj.first))
        any(adapterFor(type2).toSExpr(obj.second))
    }

    private fun <T> toPair(type1: KType, type2: KType, expr: SList): T {
        val p = adapterFor(type1).fromSExpr(expr.exprs[0])
        val q = adapterFor(type2).fromSExpr(expr.exprs[1])
        @Suppress("UNCHECKED_CAST")
        return Pair(p, q) as T
    }

    private fun fromMap(type1: KType, type2: KType, obj: Map<*, *>): SList = buildSExpr {
        obj.forEach {
            list {
                any(adapterFor(type1).toSExpr(it.key))
                any(adapterFor(type2).toSExpr(it.value))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toMap(type1: KType, type2: KType, expr: SList): T = buildMap {
        val a1 = adapterFor(type1)
        val a2 = adapterFor(type2)
        expr.exprs.forEach {
            val e = it.requireList().exprs
            val p = a1.fromSExpr(e[0])
            val q = a2.fromSExpr(e[1])
            put(p, q)
        }
    } as T

    companion object {
        /**
         * Pseudo ctor with initializer function.
         */
        operator fun invoke(initializer: SerdeRegistry.() -> Unit = {}): Serdes = Serdes().apply {
            SerdeRegistry(this).initializer()
        }
    }
}