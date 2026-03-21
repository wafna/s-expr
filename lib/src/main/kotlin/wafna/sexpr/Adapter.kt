package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

interface Adapter<T> {
    fun toSExpr(obj: T): SExpr
    fun fromSExpr(expr: SExpr): T
}

/**
 * A collection of adapters for translating objects to and from s-expressions.
 */
class Adapters {
    private val adapters = mutableMapOf<Class<*>, Adapter<*>>().apply {
        put(String::class.java, object : Adapter<String> {
            override fun toSExpr(obj: String): SExpr =
                SAtom(obj.toByteArray())

            override fun fromSExpr(expr: SExpr): String =
                expr.requireAtom().string()
        })
        put(Boolean::class.java, object : Adapter<Boolean> {
            override fun toSExpr(obj: Boolean): SExpr =
                SAtom(obj.toString().toByteArray())

            override fun fromSExpr(expr: SExpr): Boolean =
                expr.requireAtom().string().run { toBooleanStrictOrNull() ?: error("Expected Boolean, got $this.") }
        })
        put(Int::class.java, object : Adapter<Int> {
            override fun toSExpr(obj: Int): SExpr =
                SAtom(obj.toString().toByteArray())

            override fun fromSExpr(expr: SExpr): Int =
                expr.requireAtom().string().run { toIntOrNull() ?: error("Expected Int, got $this.") }
        })
        put(Long::class.java, object : Adapter<Long> {
            override fun toSExpr(obj: Long): SExpr =
                SAtom(obj.toString().toByteArray())

            override fun fromSExpr(expr: SExpr): Long =
                expr.requireAtom().string().run {
                    toLongOrNull() ?: error("Expected Long, got $this")
                }
        })
        put(Double::class.java, object : Adapter<Double> {
            override fun toSExpr(obj: Double): SExpr =
                SAtom(obj.toString().toByteArray())

            override fun fromSExpr(expr: SExpr): Double =
                expr.requireAtom().string().run {
                    toDoubleOrNull() ?: error("Expected Double, got $this")
                }
        })
    }

    inline fun <reified T : Any> adapt(): Adapter<T> =
        adapt(T::class)

    fun <T : Any> adapt(kClass: KClass<T>): Adapter<T> {
        require(kClass.isData) { "${kClass.qualifiedName} is not a data class" }
        val ctor = kClass.primaryConstructor ?: error("No primary constructor found for ${kClass.qualifiedName}")
        val adaptersByName = buildMap {
            ctor.parameters.forEach { param ->
                require(!param.isVararg) { "Varargs not supported." }
                val klass = param.type.classifier as KClass<*>
                val adapter =
                    adapters[klass.java] ?: error("No adapter found for ${klass.qualifiedName} on param ${param.name}.")
                put(param.name!!, adapter)
            }
        }
        val paramsByName = ctor.parameters.associateBy { it.name }
        return object : Adapter<T> {
            override fun toSExpr(obj: T): SExpr = buildSExpr {
                adaptersByName.forEach { (name, adapter) ->
                    val property = kClass.memberProperties.firstOrNull { it.name == name }
                        ?: error("${kClass.qualifiedName} has no property with name '$name'")
                    val value = property.get(obj)
                    val expr = (adapter.javaClass.methods.find { it.name == "toSExpr" }!!)(adapter, value) as SExpr
                    list {
                        atom(name.toByteArray(Charsets.UTF_8))
                        any(expr)
                    }
                }
            }

            override fun fromSExpr(expr: SExpr): T = when (expr) {
                is SAtom -> error("List required.")
                is SList -> {
                    val params = buildMap {
                        expr.exprs.forEach { expr ->
                            val list = expr.requireList().exprs
                            require(2 == list.size) { "Malformed value entry." }
                            val name = list[0].requireAtom().string()
                            val param = paramsByName[name] ?: error("Unknown param $name")
                            val adapter = adaptersByName[name] ?: error("Unknown param $name")
                            val s = (adapter.javaClass.methods.find { it.name == "fromSExpr" }!!)(adapter, list[1])
                            put(param, s)
                        }
                    }
                    ctor.callBy(params)
                }
            }
        }
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