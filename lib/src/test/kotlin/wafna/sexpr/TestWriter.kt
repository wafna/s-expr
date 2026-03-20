package wafna.sexpr

import kotlin.test.Test
import java.io.ByteArrayOutputStream

class TestWriter {
    fun testExpr(f: SExprBuilder.() -> Unit) {
        val expr = buildSExpr(f)
        for (fmt in DataFormat.entries) {
            val text = expr.write(ByteArrayOutputStream()) {
                indent = 2
                dataFormat = fmt
            }.toString()
//            println(text)
            compareSExpr(expr, parse(text.toCharStream()))
        }
    }
    @Test
    fun test() {
        testExpr {}
        testExpr { +"a" }
        testExpr { +"herp"; +"derp" }
        testExpr { +"sentence\r\n"; +"more\n\t" }
        testExpr { list { +"a" ; +"b" } ; list { +"c" ; +"d" } }
    }
}