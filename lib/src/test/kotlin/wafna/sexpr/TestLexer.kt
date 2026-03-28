package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals

class TestLexer {
    internal fun withLexer(input: String, f: (Lexer) -> Unit) =
        f(lexer(ByteStream.from(input)))
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
        withLexer("[\"blab\"]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertAtom("blab".bytes(), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[\"blab\\\"\\\\\\n\"]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertAtom("blab\"\\\n".bytes(), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
        withLexer("[bing bang[ boom]]") { lexer ->
            assertEquals(Token.LBracket, lexer.nextToken())
            assertAtom("bing".bytes(), lexer.nextToken())
            assertAtom("bang".bytes(), lexer.nextToken())
            assertEquals(Token.LBracket, lexer.nextToken())
            assertAtom("boom".bytes(), lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.RBracket, lexer.nextToken())
            assertEquals(Token.EOF, lexer.nextToken())
        }
    }

    companion object {
        internal fun assertAtom(expected: ByteArray, token: Token) {
            when (token) {
                is Token.LString -> assertEquals(expected.string(), token.value.string())
                else -> error("Expected LString")
            }
        }
    }
}