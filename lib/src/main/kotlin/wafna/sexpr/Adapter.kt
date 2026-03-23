package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/**
 * Uses reflection to invoke its members in order to let the JVM sort out the types,
 * which types we don't know.
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
 * A collection of adapters for translating objects to and from s-expressions.
 * Create an instance, register data classes, invoke toSExpr and fromSExpr.
 * Primitives, (registered) data classes, and lists thereof are handled.
 */
class Adapters {
    private val adapters = mutableMapOf<Class<*>, Adapter<*>>().apply {
        // Add all the primitive adapters.
        put(Byte::class.java, object : Adapter<Byte> {
            override fun toSExpr(obj: Byte): SExpr = SAtom(ByteArray(1).also { it[0] = obj })
            override fun fromSExpr(expr: SExpr): Byte = expr.requireAtom().data[0]
        })
        put(Char::class.java, object : Adapter<Char> {
            override fun toSExpr(obj: Char): SExpr = SAtom(ByteArray(1).also { it[0] = obj.code.toByte() })
            override fun fromSExpr(expr: SExpr): Char = expr.requireAtom().data[0].toInt().toChar()
        })
        put(String::class.java, object : Adapter<String> {
            override fun toSExpr(obj: String): SExpr = SAtom(obj.toByteArray())
            override fun fromSExpr(expr: SExpr): String = expr.requireAtom().string()
        })
        put(Boolean::class.java, object : Adapter<Boolean> {
            override fun toSExpr(obj: Boolean): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Boolean =
                expr.requireAtom().string().run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        })
        put(Int::class.java, object : Adapter<Int> {
            override fun toSExpr(obj: Int): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Int =
                expr.requireAtom().string().run { toIntOrNull() ?: error("Expected Int, got $this.") }
        })
        put(Long::class.java, object : Adapter<Long> {
            override fun toSExpr(obj: Long): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Long =
                expr.requireAtom().string().run { toLongOrNull() ?: error("Expected Long, got $this") }
        })
        put(Double::class.java, object : Adapter<Double> {
            override fun toSExpr(obj: Double): SExpr = SAtom(obj.toString().toByteArray())
            override fun fromSExpr(expr: SExpr): Double =
                expr.requireAtom().string().run { toDoubleOrNull() ?: error("Expected Double, got $this") }
        })
    }

    private fun adapter(kType: KType): Adapter<*> {
        val kClass = kType.classifier as KClass<*>
        return if (kClass == List::class) {
            listAdapter(kType.arguments.first().type!!)
        } else adapters[kClass.java] ?: error("No adapter for type $kClass")
    }

    /**
     * Create an adapter for a data class.
     * This class may now appear in supported collections.
     */
    inline fun <reified T : Any> register() = register<T>(typeOf<T>())

    @PublishedApi
    internal fun <T : Any> register(kType: KType) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        require(kClass.isData) { "${kClass.qualifiedName} is not a data class" }
        val ctor = kClass.primaryConstructor ?: error("No primary constructor found for ${kClass.qualifiedName}")
        val adaptersByName = buildMap {
            ctor.parameters.forEach { param ->
                require(!param.isVararg) { "Varargs not supported." }
                val klass = param.type.classifier as KClass<*>
                if (klass == List::class) {
                    put(param.name!!, listAdapter(param.type.arguments.first().type!!))
                } else {
                    val adapter = adapter(param.type)
                    put(param.name!!, adapter)
                }
            }
        }

        val paramsByName = ctor.parameters.associateBy { it.name }
        adapters[kClass.java] = object : Adapter<T> {
            override fun toSExpr(obj: T): SExpr = buildSExpr {
                adaptersByName.forEach { (name, adapter) ->
                    val property = kClass.memberProperties.firstOrNull { it.name == name }
                        ?: error("${kClass.qualifiedName} has no property with name '$name'")
                    val value = property.get(obj)
                    val expr = adapter.invokeTo(value)
                    list {
                        atom(name.toByteArray(Charsets.UTF_8))
                        any(expr)
                    }
                }
            }

            override fun fromSExpr(expr: SExpr): T = expr.requireList().run {
                ctor.callBy(buildMap {
                    exprs.forEach { expr ->
                        val list = expr.requireList().exprs
                        require(2 == list.size) { "Malformed value entry: ${list.size}" }
                        val name = list[0].requireAtom().string()
                        val param = paramsByName[name] ?: error("Unknown param $name")
                        val adapter = adaptersByName[name] ?: error("Unknown param $name")
                        val s = adapter.invokeFrom(list[1])
                        put(param, s)
                    }
                })
            }
        }
    }

    private fun listAdapter(kt: KType): Adapter<List<*>> = object : Adapter<List<*>> {
        override fun toSExpr(obj: List<*>): SExpr = toSList(kt, obj)
        override fun fromSExpr(expr: SExpr): List<*> = fromSList(kt, expr.requireList())
    }

    /**
     * Render an object as an s-expression.
     */
    inline fun <reified T> toSExpr(obj: T): SExpr = toSExpr(typeOf<T>(), obj)

    @PublishedApi
    internal fun <T> toSExpr(kType: KType, obj: T): SExpr {
        val kClass = kType.classifier as KClass<*>
        return if (kClass.isData) {
            adapter(kType).invokeTo(obj)
        } else if (kClass == List::class) {
            toSList(kType.arguments.first().type!!, obj as List<*>)
        } else error("Required data class or list.")
    }

    /**
     * Create an object from an s-expression.
     */
    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)

    @PublishedApi
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T {
        val kClass = kType.classifier as KClass<*>
        return if (kClass.isData) {
            val adapter = adapter(kType)
            @Suppress("UNCHECKED_CAST")
            adapter.invokeFrom(expr) as T
        } else if (kClass == List::class) {
            fromSList<T>(kType.arguments.first().type!!, expr.requireList())
        } else error("Required data class or list.")
    }

    private fun toSList(itemType: KType, obj: List<*>): SList {
        val adapter = adapter(itemType)
        return buildSExpr {
            obj.forEach { any(adapter.invokeTo(it)) }
        }
    }

    private fun <T> fromSList(kType: KType, expr: SList): T {
        val adapter = adapter(kType)
        @Suppress("UNCHECKED_CAST")
        return buildList {
            expr.requireList().exprs.forEach { add(adapter.invokeFrom(it)) }
        } as T
    }
}

private fun SExpr.requireList(msg: String = "Expected list."): SList = when (this) {
    is SAtom -> error(msg)
    is SList -> this
}

private fun SExpr.requireAtom(msg: String = "Expected atom."): SAtom = when (this) {
    is SAtom -> this
    is SList -> error(msg)
}

private fun SAtom.string(): String = String(data, Charsets.UTF_8)