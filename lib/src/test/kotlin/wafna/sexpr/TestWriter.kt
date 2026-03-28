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
            compareSExpr(expr, readSExpr(ByteStream.from(text)))
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
        buildSExpr { atom("a"); atom("b") }.let { s ->
            val string = s.showSExpr()
            assertEquals("[1:a1:b]", string)
        }
        buildSExpr { atom("a"); atom("b") }.let { s ->
            val string = s.showSExpr { dataFormat = DataFormat.Readable }
            assertEquals("[a b]", string)
        }
        buildSExpr { atom("a"); atom("b c"); atom("d") }.let { s ->
            val string = s.showSExpr { dataFormat = DataFormat.Readable }
            assertEquals("[a \"b c\" d]", string)
        }
        // Chooses bare, string, and rle appropriately.
        buildSExpr { atom("a"); atom("b c"); atom("d\u001e") }.let { s ->
            val string = s.showSExpr { dataFormat = DataFormat.Readable }
            assertEquals("[a \"b c\" 2:d\u001e]", string)
        }
    }
}