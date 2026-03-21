package wafna.sexpr

import kotlin.math.PI
import kotlin.test.Test
import java.io.ByteArrayOutputStream

data class Thing1(
    val name: String,
    val count: Int,
    val value: Double
)

data class Thing2(
    val names: List<String>
)

class TestAdapter {
    @Test
    fun thing1() {
        val adapters = Adapters()
        val adapter = adapters.adapt<Thing1>()
        val expr = adapter.toSExpr(Thing1("foo", 42, PI))
        println(expr.write(ByteArrayOutputStream()).toString())
        val thing = adapter.fromSExpr(expr)
        println(thing)
    }
    @Test
    fun thing2() {
        val adapters = Adapters()
        adapters.adapt<List<String>>()
        val adapter = adapters.adapt<Thing2>()
        val expr = adapter.toSExpr(Thing2(listOf("foo", "bar")))
        println(expr.write(ByteArrayOutputStream()).toString())
        val thing = adapter.fromSExpr(expr)
        println(thing)
    }
}