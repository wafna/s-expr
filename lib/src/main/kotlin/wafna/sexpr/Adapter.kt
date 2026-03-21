package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

internal interface Adapter<T> {
    fun toSExpr(obj: T): SExpr
    fun fromSExpr(expr: SExpr): T
    fun invokeTo(obj: Any?): SExpr {
        val fn = javaClass.methods.find { it.name == "toSExpr" }
            ?: error("Internal error: missing toSExpr")
        return fn(this, obj) as SExpr
    }

    fun invokeFrom(expr: SExpr): Any? {
        val fn = javaClass.methods.find { it.name == "fromSExpr" }
            ?: error("Internal error: missing fromSExpr")
        return fn(this, expr)
    }
}

/**
 * A collection of adapters for translating objects to and from s-expressions.
 */
class Adapters {
    private val adapters = mutableMapOf<Class<*>, Adapter<*>>().apply {
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

    inline fun <reified T : Any> register() =
        register<T>(typeOf<T>())

    fun <T : Any> register(kType: KType) {
        @Suppress("UNCHECKED_CAST")
        val kClass = kType.classifier as KClass<T>
        require(kClass.isData) { "${kClass.qualifiedName} is not a data class" }
        val ctor = kClass.primaryConstructor ?: error("No primary constructor found for ${kClass.qualifiedName}")
        val adaptersByName = buildMap {
            ctor.parameters.forEach { param ->
                require(!param.isVararg) { "Varargs not supported." }
                val klass = param.type.classifier as KClass<*>
                if (klass == List::class) {
                    put(param.name!!, object : Adapter<Any?> {
                        override fun toSExpr(obj: Any?): SExpr = toSList(param.type, obj)
                        override fun fromSExpr(expr: SExpr): Any? = fromSList(param.type, expr)
                    })
                } else {
                    val adapter = adapters[klass.java]
                        ?: error("No adapter found for ${klass.qualifiedName} on param ${param.name}.")
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

    inline fun <reified T> toSExpr(obj: T): SExpr = toSExpr(typeOf<T>(), obj)
    @PublishedApi
    internal fun <T> toSExpr(kType: KType, obj: T): SExpr {
        val kClass = kType.classifier as KClass<*>
        return if (kClass.isData) {
            val adapter = adapters[kClass.java] ?: error("No adapter found for ${kClass.qualifiedName}")
            adapter.invokeTo(obj)
        } else if (kClass == List::class) {
            toSList(kType, obj)
        } else error("Required data class or list.")
    }

    private fun <T> toSList(kType: KType, obj: T): SList {
        val kt = kType.arguments.first().type!!.classifier as KClass<*>
        println("LIST TYPE ${kt.java}")
        val adapter = adapters[kt.java] ?: error("No adapter found for ${kt.java} in List")
        return buildSExpr {
            (obj as List<*>).forEach {
                any(adapter.invokeTo(it))
            }
        }
    }

    inline fun <reified T> fromSExpr(expr: SExpr): T = fromSExpr(typeOf<T>(), expr)
    @PublishedApi
    internal fun <T> fromSExpr(kType: KType, expr: SExpr): T {
        val kClass = kType.classifier as KClass<*>
        @Suppress("UNCHECKED_CAST")
        return if (kClass.isData) {
            val adapter = adapters[kClass.java] ?: error("No adapter found for ${kClass.qualifiedName}")
            adapter.invokeFrom(expr) as T
        } else if (kClass == List::class) {
            fromSList<T>(kType, expr)
        } else error("Required data class or list.")
    }

    private fun <T> fromSList(kType: KType, expr: SExpr): T {
        val kt = kType.arguments.first().type!!.classifier as KClass<*>
        println("LIST TYPE ${kt.java}")
        val adapter = adapters[kt.java] ?: error("No adapter found for ${kt.java} in List")
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