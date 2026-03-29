package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf
import java.lang.reflect.Method

/**
 * Converts objects to and from s-expressions or bytes.
 */
interface Mapper<T> {
    fun toSExpr(obj: T, listener: Listener)
    fun fromSExpr(expr: SExpr): T
}

/**
 * Uses reflection to invoke its members in order to let the JVM sort out the types, which types we don't know.
 * The "actual" methods intercept NULLs, then defer to the implementation.
 * It would be nice to make the actual methods private, as well, but they seem to become invisible to reflection.
 */
private abstract class Adapter<T> : Mapper<T> {
    @Suppress("unused")
    fun actualToSExpr(obj: T?, listener: Listener) = obj?.let { toSExpr(it, listener) } ?: listener.atom(SNull)
    @Suppress("unused")
    fun actualFromSExpr(expr: SExpr): T? = if (expr == SNull) null else fromSExpr(expr)

    // Look up the targets to save one reflective step.
    private fun fn(name: String, vararg parameterTypes: Class<*>): Method =
        javaClass.getMethod(name, *parameterTypes).apply { trySetAccessible() }

    private val fnToSExpr = fn("actualToSExpr", Any::class.java, Listener::class.java)
    private val fnFromSExpr = fn("actualFromSExpr", SExpr::class.java)

    fun proxyToSExpr(obj: Any?, listener: Listener) = fnToSExpr(this, obj, listener)
    fun proxyFromSExpr(expr: SExpr): Any? = fnFromSExpr(this, expr)
}

/**
 * Exposes initialization to clients.
 */
class MapperRegistry internal constructor(val mappers: Mappers) {
    /**
     * Create a mapper for a data class or enum.
     */
    inline fun <reified T : Any> adapt() = mappers.adapt<T>(typeOf<T>())
    /**
     * Register a custom mapper for a type T.
     */
    inline fun <reified T : Any> register(mapper: Mapper<T>) = mappers.register(typeOf<T>(), mapper)
}

/**
 * A collection of mappers for translating objects to and from s-expressions.
 */
class Mappers private constructor() {
    /**
     * Render an object as an s-expression.
     */
    inline fun <reified T> toSExpr(obj: T, listener: Listener) {
        toSExpr(typeOf<T>(), obj, listener)
    }

    /**
     * Constructs the s-expression in memory.
     */
    inline fun <reified T> toSExpr(obj: T): SExpr = TreeBuilder().apply {
        toSExpr(obj, this)
    }.finish()

    @PublishedApi
    internal fun <T> toSExpr(kType: KType, obj: T, listener: Listener) {
        adapterFor(kType).proxyToSExpr(obj, listener)
    }

    /**
     * Create an object from an s-expression.
     */
    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T = adapterFor(kType).proxyFromSExpr(expr) as T

    private val adapters = mutableMapOf<KClass<*>, Adapter<*>>(
        // Primitive adapters.
        // Collection types are built on the fly, below.
        Byte::class to object : Adapter<Byte?>() {
            override fun toSExpr(obj: Byte?, listener: Listener) =
                listener.atom(obj?.let { o -> SBytes(ByteArray(1).also { it[0] = o }) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Byte = expr.mapAtom { data[0] }
        },
        Char::class to object : Adapter<Char?>() {
            override fun toSExpr(obj: Char?, listener: Listener) =
                listener.atom(obj?.let { SBytes(ByteArray(1).also { it[0] = obj.code.toByte() }) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Char = expr.mapAtom { data[0].toInt().toChar() }
        },
        String::class to object : Adapter<String?>() {
            override fun toSExpr(obj: String?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): String? = expr.requireAtom().asString()
        },
        Boolean::class to object : Adapter<Boolean?>() {
            override fun toSExpr(obj: Boolean?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Boolean? =
                expr.requireAtom().asString()?.run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        },
        Short::class to object : Adapter<Short?>() {
            override fun toSExpr(obj: Short?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Short? =
                expr.requireAtom().asString()?.run { toShortOrNull() ?: error("Expected Short, got $this.") }
        },
        Int::class to object : Adapter<Int?>() {
            override fun toSExpr(obj: Int?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Int? =
                expr.requireAtom().asString()?.run { toIntOrNull() ?: error("Expected Int, got $this.") }
        },
        Long::class to object : Adapter<Long?>() {
            override fun toSExpr(obj: Long?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Long? =
                expr.requireAtom().asString()?.run { toLongOrNull() ?: error("Expected Long, got $this") }
        },
        Double::class to object : Adapter<Double?>() {
            override fun toSExpr(obj: Double?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Double? =
                expr.requireAtom().asString()?.run { toDoubleOrNull() ?: error("Expected Double, got $this") }
        },
        Float::class to object : Adapter<Float?>() {
            override fun toSExpr(obj: Float?, listener: Listener) =
                listener.atom(obj?.let { SBytes(obj.toString().bytes()) } ?: SNull)

            override fun fromSExpr(expr: SExpr): Float? =
                expr.requireAtom().asString()?.run { toFloatOrNull() ?: error("Expected Float, got $this") }
        }
    )

    // Find an adapter for this type or die in the attempt.
    // This includes building adapters for collections using their (reified) type arguments.
    private fun adapterFor(kType: KType): Adapter<*> {
        val kClass = kType.classifier as KClass<*>
        return adapters[kClass] ?: when (kClass) {
            List::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<List<*>>() {
                    override fun toSExpr(obj: List<*>, listener: Listener) = fromList(itemType, obj, listener)
                    override fun fromSExpr(expr: SExpr): List<*> = toList(itemType, expr.requireList())
                }
            }

            Set::class -> {
                val itemType = kType.arguments.first().type!!
                object : Adapter<Set<*>>() {
                    override fun toSExpr(obj: Set<*>, listener: Listener) = fromSet(itemType, obj, listener)
                    override fun fromSExpr(expr: SExpr): Set<*> = toSet(itemType, expr.requireList())
                }
            }

            Pair::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Pair<*, *>>() {
                    override fun toSExpr(obj: Pair<*, *>, listener: Listener) = fromPair(type1, type2, obj, listener)
                    override fun fromSExpr(expr: SExpr): Pair<*, *> = toPair(type1, type2, expr.requireList())
                }
            }

            Map::class -> {
                val type1 = kType.arguments[0].type!!
                val type2 = kType.arguments[1].type!!
                object : Adapter<Map<*, *>>() {
                    override fun toSExpr(obj: Map<*, *>, listener: Listener) = fromMap(type1, type2, obj, listener)
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
    internal fun <T : Any> register(kType: KType, mapper: Mapper<T>) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        adapters[kClass] = object : Adapter<T>() {
            override fun toSExpr(obj: T, listener: Listener) = mapper.toSExpr(obj, listener)
            override fun fromSExpr(expr: SExpr): T = mapper.fromSExpr(expr)
        }
    }

    @PublishedApi
    internal fun <T : Any> adapt(kType: KType) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        if (kClass.isData) {
            adapters[kClass] = createDataAdapter(kClass)
        } else if (kClass.isSealed) {
            registerSealed(kClass)
        } else if (kClass.isSubclassOf(Enum::class)) {
            registerEnum(kClass)
        } else {
            error("Data class, sealed class, or enum required.")
        }
    }

    /**
     * Create an adapter for a data class.
     */
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
            override fun toSExpr(obj: T, listener: Listener) {
                with(listener) {
                    list {
                        adaptersByName.forEach { (name, adapter) ->
                            list {
                                atom(SBytes(name.bytes()))
                                val property = propertiesByName[name]
                                    ?: error("${kClass.qualifiedName} has no property with name '$name'")
                                val value = property.get(obj)
                                adapter.proxyToSExpr(value, listener)
                            }
                        }
                    }
                }
            }

            override fun fromSExpr(expr: SExpr): T = expr.requireList().run {
                ctor.callBy(buildMap {
                    exprs.forEach { expr ->
                        val list = expr.requireList().exprs
                        require(2 == list.size) { "Malformed value entry: ${list.size}" }
                        val name = list[0].mapAtom { asString() }!!
                        val param = paramsByName[name] ?: error("Unknown param $name on $kClass")
                        val adapter = adaptersByName[name] ?: error("Unknown param $name on $kClass")
                        val s = adapter.proxyFromSExpr(list[1])
                        put(param, s)
                    }
                })
            }
        }
    }

    /**
     * Creates adapters for a sealed hierarchy rooted at the given class.
     */
    private fun registerSealed(kClass: KClass<*>) {
        require(kClass.isSealed) { "${kClass.qualifiedName} is not a sealed class." }
        val typeAdapters = buildMap {
            kClass.sealedSubclasses.forEach { subClass ->
                if (subClass.isData && !adapters.contains(subClass)) {
                    val adapter = createDataAdapter(subClass)
                    adapters[subClass] = adapter
                    put(subClass.simpleName!!, adapter)
                } else if (subClass.isSealed) {
                    registerSealed(subClass)
                } else {
                    error("Only sealed and data classes allowed in hierarchy: ${subClass.qualifiedName}")
                }
            }
        }
        if (!adapters.contains(kClass)) {
            adapters[kClass] = object : Adapter<Any>() {
                override fun toSExpr(obj: Any, listener: Listener) {
                    listener.list {
                        val objClass = obj::class
                        val typeName = objClass.simpleName!!
                        listener.atom(SBytes(typeName.bytes()))
                        typeAdapters.getValue(typeName).proxyToSExpr(obj, listener)
                    }
                }

                override fun fromSExpr(expr: SExpr): Any {
                    val items = expr.requireList().exprs
                    val type = items[0].mapAtom { asString() }!!
                    val adapter = typeAdapters.getValue(type)
                    @Suppress("UNCHECKED_CAST")
                    return adapter.proxyFromSExpr(items[1].requireList()) as Any
                }
            }
        }
    }

    /**
     * Register an adapter for an enum.
     */
    private fun <T : Any> registerEnum(kClass: KClass<T>) {
        val byName = buildMap {
            kClass.java.enumConstants.forEach {
                put(it.toString(), it)
            }
        }
        adapters[kClass] = object : Adapter<T>() {
            override fun toSExpr(obj: T, listener: Listener) =
                listener.atom(SBytes(obj.toString().bytes()))

            override fun fromSExpr(expr: SExpr): T =
                byName.getValue(expr.mapAtom { asString() }!!) as T
        }
    }

    // Collection handlers.

    private fun fromList(itemType: KType, obj: List<*>, listener: Listener) {
        val adapter = adapterFor(itemType)
        listener.list {
            obj.forEach { adapter.proxyToSExpr(it, listener) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toList(itemType: KType, expr: SList): T = buildList {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.proxyFromSExpr(it)) }
    } as T

    private fun fromSet(itemType: KType, obj: Set<*>, listener: Listener) {
        val adapter = adapterFor(itemType)
        listener.list {
            obj.forEach { adapter.proxyToSExpr(it, listener) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toSet(itemType: KType, expr: SList): T = buildSet {
        val adapter = adapterFor(itemType)
        expr.requireList().exprs.forEach { add(adapter.proxyFromSExpr(it)) }
    } as T

    private fun fromPair(type1: KType, type2: KType, obj: Pair<*, *>, listener: Listener) {
        listener.list {
            adapterFor(type1).proxyToSExpr(obj.first, listener)
            adapterFor(type2).proxyToSExpr(obj.second, listener)
        }
    }

    private fun <T> toPair(type1: KType, type2: KType, expr: SList): T {
        val p = adapterFor(type1).proxyFromSExpr(expr.exprs[0])
        val q = adapterFor(type2).proxyFromSExpr(expr.exprs[1])
        @Suppress("UNCHECKED_CAST")
        return Pair(p, q) as T
    }

    private fun fromMap(type1: KType, type2: KType, obj: Map<*, *>, listener: Listener) {
        listener.list {
            obj.forEach {
                listener.list {
                    adapterFor(type1).proxyToSExpr(it.key, listener)
                    adapterFor(type2).proxyToSExpr(it.value, listener)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> toMap(type1: KType, type2: KType, expr: SList): T = buildMap {
        val a1 = adapterFor(type1)
        val a2 = adapterFor(type2)
        expr.exprs.forEach {
            val e = it.requireList().exprs
            val p = a1.proxyFromSExpr(e[0])
            val q = a2.proxyFromSExpr(e[1])
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