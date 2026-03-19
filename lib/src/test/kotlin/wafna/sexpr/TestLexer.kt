package wafna.sexpr

import com.google.common.io.CharStreams
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLexer {
    fun withLexer(input: String, f: (Lexer) -> Unit) =
        f(lexer(CharStream.fromString(input)))
    @Test
    fun test() {
        withLexer("[") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[ ]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
    }
}