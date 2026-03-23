package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

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

data class Thing3(val groups: List<List<String>>)

class TestAdapter {
    @Test
    fun thing1() {
        val adapters = Adapters().apply {
            register<Thing1>()
        }
        val obj = Thing1("foo", 42, PI)
        val expr = adapters.toSExpr(obj)
        assertEquals(obj, adapters.fromSExpr<Thing1>(expr))
    }
    @Test
    fun thing2() {
        val adapters = Adapters().apply {
            register<Thing2>()
        }
        val obj = Thing2(
            listOf("foo", "bar"),
            listOf(1, 2, 3),
            listOf(PI, E)
        )
        val expr = adapters.toSExpr<Thing2>(obj)
        assertEquals(obj, adapters.fromSExpr<Thing2>(expr))
    }
    @Test
    fun thing3() {
        val adapters = Adapters().apply {
            register<Thing3>()
        }
        val obj = Thing3(
            listOf(
                listOf("bing", "bang", "boom"),
                listOf("herp", "derp"),
            )
        )
        val expr = adapters.toSExpr<Thing3>(obj)
        assertEquals(obj, adapters.fromSExpr<Thing3>(expr))
    }
    @Test
    fun lists() {
        val adapters = Adapters()
        val list = listOf(1, 2, 3)
        val expr = adapters.toSExpr(list)
        println(expr.writeToString())
        assertEquals(list, adapters.fromSExpr<List<Int>>(expr))
    }
}