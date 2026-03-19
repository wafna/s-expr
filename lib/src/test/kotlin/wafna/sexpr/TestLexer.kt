package wafna.sexpr

import com.google.common.io.CharStreams
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLexer {
    @Test
    fun test() {
        val lexer = lexer(CharStream.fromString("["))
        assertEquals(Token.LBracket, lexer.nextToken())
        assertEquals(Token.EOF, lexer.nextToken())
    }
}