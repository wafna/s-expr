package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf
import java.lang.reflect.Method

/**
 * Converts objects to and from s-expressions (not strings or bytes).
 */
interface Mapper<T> {
    fun toSExpr(obj: T): SExpr
    fun fromSExpr(expr: SExpr): T
}

/**
 * Uses reflection to invoke its members in order to let the JVM sort out the types, which types we don't know.
 * The "actual" methods intercept NULLs, then defer to the implementation.
 * It would be nice to make the actual methods private, as well, but they seem to become invisible to reflection.
 */
private abstract class Adapter<T> : Mapper<T> {
    @Suppress("unused")
    fun actualTo(obj: T?): SExpr = obj?.let { toSExpr(it) } ?: SNull
    @Suppress("unused")
    fun actualFrom(expr: SExpr): T? = if (expr == SNull) null else fromSExpr(expr)

    // Look up the targets to save one reflective step.
    private fun fn(name: String, vararg parameterTypes: Class<*>): Method =
        javaClass.getMethod(name, *parameterTypes).apply { trySetAccessible() }

    private val fnTo = fn("actualTo", Any::class.java)
    private val fnFrom = fn("actualFrom", SExpr::class.java)

    fun proxyTo(obj: Any?): SExpr = fnTo(this, obj) as SExpr
    fun proxyFrom(expr: SExpr): Any? = fnFrom(this, expr)
}

/**
 * Exposes initialization to clients.
 */
class MapperRegistry internal constructor(val mappers: Mappers) {
    /**
     * Create a mapper for a data class.
     */
    inline fun <reified T : Any> register() = mappers.register<T>(typeOf<T>())
    /**
     * Register a custom mapper for a type T.
     */
    inline fun <reified T : Any> register(mapper: Mapper<T>) = mappers.adapt(typeOf<T>(), mapper)
}

/**
 * A collection of mappers for translating objects to and from s-expressions.
 */
class Mappers private constructor() {

    private val adapters = mutableMapOf<KClass<*>, Adapter<*>>(
        // Primitive adapters.
        // Collection types are built on the fly, below.
        Byte::class to object : Adapter<Byte?>() {
            override fun toSExpr(obj: Byte?): SExpr = obj?.let { o -> SBytes(ByteArray(1).also { it[0] = o }) } ?: SNull
            override fun fromSExpr(expr: SExpr): Byte? = expr.mapAtom { data[0] }
        },
        Char::class to object : Adapter<Char?>() {
            override fun toSExpr(obj: Char?): SExpr = obj?.let {
                SBytes(ByteArray(1).also {
                    it[0] = obj.code.toByte()
                })
            } ?: SNull

            override fun fromSExpr(expr: SExpr): Char? = expr.mapAtom { data[0].toInt().toChar() }
        },
        String::class to object : Adapter<String?>() {
            override fun toSExpr(obj: String?): SExpr = obj?.let { SBytes(obj.toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): String? = expr.requireAtom().asString()
        },
        Boolean::class to object : Adapter<Boolean?>() {
            override fun toSExpr(obj: Boolean?): SExpr = obj?.let { SBytes(obj.toString().toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): Boolean? =
                expr.requireAtom().asString()?.run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        },
        Int::class to object : Adapter<Int?>() {
            override fun toSExpr(obj: Int?): SExpr = obj?.let { SBytes(obj.toString().toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): Int? =
                expr.requireAtom().asString()?.run { toIntOrNull() ?: error("Expected Int, got $this.") }
        },
        Long::class to object : Adapter<Long?>() {
            override fun toSExpr(obj: Long?): SExpr = obj?.let { SBytes(obj.toString().toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): Long? =
                expr.requireAtom().asString()?.run { toLongOrNull() ?: error("Expected Long, got $this") }
        },
        Double::class to object : Adapter<Double?>() {
            override fun toSExpr(obj: Double?): SExpr = obj?.let { SBytes(obj.toString().toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): Double? =
                expr.requireAtom().asString()?.run { toDoubleOrNull() ?: error("Expected Double, got $this") }
        },
        Float::class to object : Adapter<Float?>() {
            override fun toSExpr(obj: Float?): SExpr = obj?.let { SBytes(obj.toString().toByteArray()) } ?: SNull
            override fun fromSExpr(expr: SExpr): Float? =
                expr.requireAtom().asString()?.run { toFloatOrNull() ?: error("Expected Float, got $this") }
        }
    )

    // Find an adapter for this type or die in the attempt.
    // This includes building adapters for parameterized types using their (reified) type arguments.
    private fun adapterFor(kType: KType): Adapter<*> {
        val kClass = kType.classifier as KClass<*>
        return adapters[kClass] ?: when (kClass) {
            List::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<List<*>>() {
                    override fun toSExpr(obj: List<*>): SExpr = fromList(itemType, obj)
                    override fun fromSExpr(expr: SExpr): List<*> = toList(itemType, expr.requireList())
                }
            }

            Set::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<Set<*>>() {
                    override fun toSExpr(obj: Set<*>): SExpr = fromSet(itemType, obj)
                    override fun fromSExpr(expr: SExpr): Set<*> = toSet(itemType, expr.requireList())
                }
            }

            Pair::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Pair<*, *>>() {
                    override fun toSExpr(obj: Pair<*, *>): SExpr = fromPair(type1, type2, obj)
                    override fun fromSExpr(expr: SExpr): Pair<*, *> = toPair(type1, type2, expr.requireList())
                }
            }

            Map::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Map<*, *>>() {
                    override fun toSExpr(obj: Map<*, *>): SExpr = fromMap(type1, type2, obj)
                    override fun fromSExpr(expr: SExpr): Map<*, *> = toMap(type1, type2, expr.requireList())
                }
            }

            else -> error(
                "Unsupported adapter type ${kClass.qualifiedName}${
                    adapters.toList().joinToString { "\n\t${it.first}" }
                }"
            )
        }
    }

    @PublishedApi
    internal fun <T : Any> adapt(kType: KType, mapper: Mapper<T>) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        adapters[kClass] = object : Adapter<T>() {
            override fun toSExpr(obj: T): SExpr = mapper.toSExpr(obj)
            override fun fromSExpr(expr: SExpr): T = mapper.fromSExpr(expr)
        }
    }

    @PublishedApi
    internal fun <T : Any> register(kType: KType) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        if (kClass.isData) {
            adapters[kClass] = createDataAdapter(kClass)
        } else if (kClass.isSealed) {
            // One level sealed hierarchy of data classes.
            kClass.sealedSubclasses.filter { !it.isData }.run {
                require(isEmpty()) {
                    "Sealed class must contain only data classes: ${joinToString(", ") { it.qualifiedName.toString() }}"
                }
            }
            val typeAdapters = buildMap {
                kClass.sealedSubclasses.forEach { subClass ->
                    val adapter = createDataAdapter(subClass)
                    adapters[subClass] = adapter
                    put(subClass.simpleName!!, adapter)
                }
            }
            adapters[kClass] = object : Adapter<T>() {
                override fun toSExpr(obj: T): SExpr = buildSExpr {
                    val objClass = obj::class
                    val typeName = objClass.simpleName!!
                    atom(typeName)
                    expr(typeAdapters.getValue(typeName).proxyTo(obj))
                }

                override fun fromSExpr(expr: SExpr): T {
                    val items = expr.requireList().exprs
                    val type = items[0].mapAtom { asString() }!!
                    val adapter = typeAdapters.getValue(type)
                    @Suppress("UNCHECKED_CAST")
                    return adapter.proxyFrom(items[1].requireList()) as T
                }
            }
        } else if (kClass.isSubclassOf(Enum::class)) {
            val byName = buildMap {
                kClass.java.enumConstants.forEach {
                    put(it.toString(), it)
                }
            }
            adapters[kClass] = object : Adapter<T>() {
                override fun toSExpr(obj: T): SExpr =
                    SBytes(obj.toString().toByteArray())

                override fun fromSExpr(expr: SExpr): T =
                    byName.getValue(expr.mapAtom { asString() }!!) as T
            }
        } else {
            error("Data class or sealed class required.")
        }
    }

    private fun <T : Any> createDataAdapter(kClass: KClass<T>): Adapter<T> {
        require(kClass.isData) { "Data class required: $kClass" }
        // Assume a one-to-one correspondence between the ctor and the object properties.
        val ctor = kClass.primaryConstructor ?: error("No primary constructor found for ${kClass.qualifiedName}")
        // Cache some lookups for fast serialization, below.
        val adaptersByName: Map<String, Adapter<*>> = buildMap {
            ctor.parameters.forEach { param ->
                require(!param.isVararg) { "Varargs not supported." }
                put(param.name!!, adapterFor(param.type))
            }
        }
        val propertiesByName = kClass.memberProperties.associateBy { it.name }
        val paramsByName = ctor.parameters.associateBy { it.name }
        return object : Adapter<T>() {
            override fun toSExpr(obj: T): SExpr = buildSExpr {
                println("toSExpr: $kClass")
                adaptersByName.forEach { (name, adapter) ->
                    list {
                        atom(name.toByteArray(Charsets.UTF_8))
                        val property = propertiesByName[name]
                            ?: error("${kClass.qualifiedName} has no property with name '$name'")
                        val value = property.get(obj)
                        val expr = adapter.proxyTo(value)
                        expr(expr)
                    }
                }
            }

            override fun fromSExpr(expr: SExpr): T = expr.requireList().run {
                println("fromSExpr: $kClass")
                ctor.callBy(buildMap {
                    exprs.forEach { expr ->
                        val list = expr.requireList().exprs
                        require(2 == list.size) { "Malformed value entry: ${list.size}" }
                        val name = list[0].mapAtom { asString() }!!
                        val param = paramsByName[name] ?: error("Unknown param $name on $kClass")
                        val adapter = adaptersByName[name] ?: error("Unknown param $name on $kClass")
                        val s = adapter.proxyFrom(list[1])
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
    internal fun <T> toSExpr(kType: KType, obj: T): SExpr = adapterFor(kType).proxyTo(obj)

    /**
     * Create an object from an s-expression.
     */
    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T = adapterFor(kType).proxyFrom(expr) as T

    // Collection handlers.

    private fun fromList(itemType: KType, obj: List<*>): SList {
        val adapter = adapterFor(itemType)
        return buildSExpr {
            obj.forEach { expr(adapter.proxyTo(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toList(itemType: KType, expr: SList): T = buildList {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.proxyFrom(it)) }
    } as T

    private fun fromSet(itemType: KType, obj: Set<*>): SList {
        val adapter = adapterFor(itemType)
        return buildSExpr {
            obj.forEach { expr(adapter.proxyTo(it)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toSet(itemType: KType, expr: SList): T = buildSet {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.proxyFrom(it)) }
    } as T

    private fun fromPair(type1: KType, type2: KType, obj: Pair<*, *>): SList = buildSExpr {
        expr(adapterFor(type1).proxyTo(obj.first))
        expr(adapterFor(type2).proxyTo(obj.second))
    }

    private fun <T> toPair(type1: KType, type2: KType, expr: SList): T {
        val p = adapterFor(type1).proxyFrom(expr.exprs[0])
        val q = adapterFor(type2).proxyFrom(expr.exprs[1])
        @Suppress("UNCHECKED_CAST")
        return Pair(p, q) as T
    }

    private fun fromMap(type1: KType, type2: KType, obj: Map<*, *>): SList = buildSExpr {
        obj.forEach {
            list {
                expr(adapterFor(type1).proxyTo(it.key))
                expr(adapterFor(type2).proxyTo(it.value))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toMap(type1: KType, type2: KType, expr: SList): T = buildMap {
        val a1 = adapterFor(type1)
        val a2 = adapterFor(type2)
        expr.exprs.forEach {
            val e = it.requireList().exprs
            val p = a1.proxyFrom(e[0])
            val q = a2.proxyFrom(e[1])
            put(p, q)
        }
    } as T

    companion object {
        /**
         * Pseudo ctor with initializer function.
         */
        operator fun invoke(initializer: MapperRegistry.() -> Unit = {}): Mappers = Mappers().apply {
            MapperRegistry(this).initializer()
        }
    }
}