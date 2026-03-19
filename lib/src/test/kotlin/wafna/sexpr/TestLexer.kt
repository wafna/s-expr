package wafna.sexpr

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
        withLexer("12") { lexer ->
            assertEquals(Token.LInteger(12), lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("-12.4") { lexer ->
            assertEquals(Token.LDouble(-12.4), lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[\"blab\"]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.LString("blab"), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[\"blab\\\"\\\\\\n\"]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.LString("blab\"\\\n"), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[bing bang[ boom]]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.LString("bing"), lexer.nextToken())
            assertEquals(Token.LString("bang"), lexer.nextToken())
            assertEquals(Token.LBracket, lexer.nextToken())
            assertEquals(Token.LString("boom"), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
    }
}