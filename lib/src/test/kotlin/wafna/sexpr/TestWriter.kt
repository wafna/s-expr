package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import java.io.ByteArrayOutputStream

fun compareSExpr(actual: SExpr, expected: SExpr) {
    when (actual) {
        is SAtom -> when (expected) {
            is SAtom -> assertEquals(expected.data.toString(Charsets.UTF_8), actual.data.toString(Charsets.UTF_8))
            is SList -> fail("Incompatible: expected SList, got SAtom")
        }

        is SList -> when (expected) {
            is SList -> {
                assertEquals(expected.exprs.size, actual.exprs.size)
                expected.exprs.zip(actual.exprs).forEach { (a, b) ->
                    compareSExpr(a, b)
                }
            }

            is SAtom -> fail("Incompatible: expected SAtom, got SList")
        }
    }
}

class TestWriter {
    fun testExpr(f: SExprBuilder.() -> Unit) {
        val expr = buildSExpr(f)
        val text = expr.write(ByteArrayOutputStream()).toString()
        println(text)
        compareSExpr(expr, parse(text.toCharStream()))
    }
    @Test
    fun test() {
        testExpr {}
        testExpr { +"a" }
        testExpr { +"herp"; +"derp" }
        testExpr { +"sentence\r\n"; +"more\n\t" }
    }
}