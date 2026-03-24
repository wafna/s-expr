package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/**
 * Uses reflection to invoke its members in order to let the JVM sort out the types, which types we don't know.
 */
private interface Adapter<T> {
    fun toSExpr(obj: T): SExpr
    fun fromSExpr(expr: SExpr): T

    fun invokeTo(obj: Any?): SExpr = (javaClass.methods.find { it.name == "toSExpr" }
        ?: error("Internal error: missing toSExpr"))(this, obj) as SExpr

    fun invokeFrom(expr: SExpr): Any? = (javaClass.methods.find { it.name == "fromSExpr" }
        ?: error("Internal error: missing fromSExpr"))(this, expr)
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
        // Collection types are build on the fly, below.
        Byte::class.java to object : Adapter<Byte> {
            override fun toSExpr(obj: Byte): SExpr = SAtom(ByteArray(1).also { it[0] = obj })
            override fun fromSExpr(expr: SExpr): Byte = expr.requireAtom().data[0]
        },
        Char::class.java to object : Adapter<Char> {
            override fun toSExpr(obj: Char): SExpr = SAtom(ByteArray(1).also { it[0] = obj.code.toByte() })
            override fun fromSExpr(expr: SExpr): Char = expr.requireAtom().data[0].toInt().toChar()
        },
        String::class.java to object : Adapter<String> {
            override fun toSExpr(obj: String): SExpr = SAtom(obj.toByteArray())
            override fun fromSExpr(expr: SExpr): String = expr.requireAtom().asName()
        },
        Boolean::class.java to object : Adapter<Boolean> {
            override fun toSExpr(obj: Boolean): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Boolean =
                expr.requireAtom().asName().run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        },
        Int::class.java to object : Adapter<Int> {
            override fun toSExpr(obj: Int): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Int =
                expr.requireAtom().asName().run { toIntOrNull() ?: error("Expected Int, got $this.") }
        },
        Long::class.java to object : Adapter<Long> {
            override fun toSExpr(obj: Long): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Long =
                expr.requireAtom().asName().run { toLongOrNull() ?: error("Expected Long, got $this") }
        },
        Double::class.java to object : Adapter<Double> {
            override fun toSExpr(obj: Double): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Double =
                expr.requireAtom().asName().run { toDoubleOrNull() ?: error("Expected Double, got $this") }
        }
    )

    // Find an adapter for this type or die in the attempt.
    // This includes building adapters for parameterized types using their (reified) type arguments.
    private fun adapter(kType: KType): Adapter<*> {
        val kClass = kType.classifier as KClass<*>
        return adapters[kClass.java] ?: when (kClass) {
            List::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<List<*>> {
                    override fun toSExpr(obj: List<*>): SExpr = fromList(itemType, obj)
                    override fun fromSExpr(expr: SExpr): List<*> = toList(itemType, expr.requireList())
                }
            }

            Set::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<Set<*>> {
                    override fun toSExpr(obj: Set<*>): SExpr = fromSet(itemType, obj)
                    override fun fromSExpr(expr: SExpr): Set<*> = toSet(itemType, expr.requireList())
                }
            }

            Pair::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Pair<*, *>> {
                    override fun toSExpr(obj: Pair<*, *>): SExpr = fromPair(type1, type2, obj)
                    override fun fromSExpr(expr: SExpr): Pair<*, *> = toPair(type1, type2, expr.requireList())
                }
            }

            Map::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Map<*, *>> {
                    override fun toSExpr(obj: Map<*, *>): SExpr = fromMap(type1, type2, obj)
                    override fun fromSExpr(expr: SExpr): Map<*, *> = toMap(type1, type2, expr.requireList())
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
                put(param.name!!, adapter(param.type))
            }
        }
        val paramsByName = ctor.parameters.associateBy { it.name }
        adapters[kClass.java] = object : Adapter<T> {
            override fun toSExpr(obj: T): SExpr = buildSExpr {
                adaptersByName.forEach { (name, adapter) ->
                    list {
                        atom(name.toByteArray(Charsets.UTF_8))
                        val property = kClass.memberProperties.firstOrNull { it.name == name }
                            ?: error("${kClass.qualifiedName} has no property with name '$name'")
                        val value = property.get(obj)
                        val expr = adapter.invokeTo(value)
                        any(expr)
                    }
                }
            }

            override fun fromSExpr(expr: SExpr): T = expr.requireList().run {
                ctor.callBy(buildMap {
                    exprs.forEach { expr ->
                        val list = expr.requireList().exprs
                        require(2 == list.size) { "Malformed value entry: ${list.size}" }
                        val name = list[0].requireAtom().asName()
                        val param = paramsByName[name] ?: error("Unknown param $name")
                        val adapter = adaptersByName[name] ?: error("Unknown param $name")
                        val s = adapter.invokeFrom(list[1])
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
    internal fun <T> toSExpr(kType: KType, obj: T): SExpr = adapter(kType).invokeTo(obj)

    /**
     * Create an object from an s-expression.
     */
    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T = adapter(kType).invokeFrom(expr) as T

    // Collection handlers.

    private fun fromList(itemType: KType, obj: List<*>): SList {
        val adapter = adapter(itemType)
        return buildSExpr {
            obj.forEach { any(adapter.invokeTo(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toList(itemType: KType, expr: SList): T = buildList {
        val adapter = adapter(itemType)
        expr.requireList().exprs.forEach { add(adapter.invokeFrom(it)) }
    } as T

    private fun fromSet(itemType: KType, obj: Set<*>): SList {
        val adapter = adapter(itemType)
        return buildSExpr {
            obj.forEach { any(adapter.invokeTo(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toSet(itemType: KType, expr: SList): T = buildSet {
        val adapter = adapter(itemType)
        expr.requireList().exprs.forEach { add(adapter.invokeFrom(it)) }
    } as T

    private fun fromPair(type1: KType, type2: KType, obj: Pair<*, *>): SList = buildSExpr {
        any(adapter(type1).invokeTo(obj.first))
        any(adapter(type2).invokeTo(obj.second))
    }

    private fun <T> toPair(type1: KType, type2: KType, expr: SList): T {
        val p = adapter(type1).invokeFrom(expr.exprs[0])
        val q = adapter(type2).invokeFrom(expr.exprs[1])
        @Suppress("UNCHECKED_CAST")
        return Pair(p, q) as T
    }

    private fun fromMap(type1: KType, type2: KType, obj: Map<*, *>): SList = buildSExpr {
        obj.forEach {
            list {
                any(adapter(type1).invokeTo(it.key))
                any(adapter(type2).invokeTo(it.value))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toMap(type1: KType, type2: KType, expr: SList): T = buildMap {
        val a1 = adapter(type1)
        val a2 = adapter(type2)
        expr.exprs.forEach {
            val e = it.requireList().exprs
            val p = a1.invokeFrom(e[0])
            val q = a2.invokeFrom(e[1])
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

        private fun SExpr.requireList(msg: String = "Expected list."): SList = when (this) {
            is SAtom -> error(msg)
            is SList -> this
        }

        private fun SExpr.requireAtom(msg: String = "Expected atom."): SAtom = when (this) {
            is SAtom -> this
            is SList -> error(msg)
        }

        private fun SAtom.asName(): String = String(data, Charsets.UTF_8)
    }
}