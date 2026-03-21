package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import java.io.ByteArrayOutputStream

data class Thing1(
    val name: String,
    val count: Int,
    val value: Double
)

data class Thing2(
    val names: List<String>,
    val counts: List<Int>,
    val values: List<Double>
)

class TestAdapter {
    @Test
    fun thing1() {
        val adapters = Adapters()
        adapters.register<Thing1>()
        val expr = adapters.toSExpr(Thing1("foo", 42, PI))
        println(expr.write(ByteArrayOutputStream()).toString())
        val thing = adapters.fromSExpr<Thing1>(expr)
        println(thing)
    }
    @Test
    fun thing2() {
        val adapters = Adapters()
        adapters.register<Thing2>()
        val expr = adapters.toSExpr<Thing2>(Thing2(
            listOf("foo", "bar"),
            listOf(1,2,3),
            listOf(PI, E)))
        println(expr.write(ByteArrayOutputStream()).toString())
        val thing = adapters.fromSExpr<Thing2>(expr)
        println(thing)
    }
}