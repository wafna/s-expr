package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.ByteArrayOutputStream

class TestWriter {
    fun testExpr(f: SExprBuilder.() -> Unit) {
        val expr = buildSExpr(f)
        for (fmt in DataFormat.entries) {
            val text = expr.write(ByteArrayOutputStream()) {
                indent = 2
                dataFormat = fmt
            }.toString()
            compareSExpr(expr, readSExpr(CharStream.from(text)))
        }
    }
    @Test
    fun test() {
        testExpr {}
        testExpr { atom("a") }
        testExpr { atom("herp"); atom("derp") }
        testExpr { atom("sentence\r\n"); atom("more\n\t") }
        testExpr {
            list { atom("a"); atom("b") }
            list { atom("c"); atom("d") }
        }
    }
    @Test
    fun testShow() {
        val s = buildSExpr { atom("a"); atom("b") }
        assertEquals("[1:a1:b]", s.showSExpr())
    }
}