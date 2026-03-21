package wafna.sexpr

import kotlin.reflect.KClass
import kotlin.test.Test
import java.io.ByteArrayOutputStream

data class Thingy(
    val name: String,
    val count: Int
)

class TestAdapter {
    @Test
    fun test() {
        val adapters = Adapters()
        val adapter = adapters.adapt<Thingy>()
        val expr = adapter.toSExpr(Thingy("foo", 42))
        println(expr.write(ByteArrayOutputStream()).toString())
        val thing = adapter.fromSExpr(expr)
        println(thing)
    }
}