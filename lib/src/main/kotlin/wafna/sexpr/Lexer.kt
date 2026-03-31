package wafna.sexpr

import java.nio.ByteBuffer
import java.util.*

/**
 * Produces tokens and byte arrays, the latter for run length encoded atoms.
 */
internal interface Lexer {
    fun nextToken(): Token
    fun nextBytes(count: Int): ByteArray
}

/**
 * Create a Lexer for an input.
 */
internal fun lexer(input: ByteStream): Lexer = object : Lexer {
    var line = 0
    var column = 0
    val currentToken = Stack<Byte>()
    fun take() {
        val b = input.take()
        if (b == Bytes.NEW_LINE) {
            line++
            column = 0
        } else {
            column++
        }
        currentToken.push(b)
    }

    private fun error(msg: String): Nothing = throw SExprError.Parse(line, column, msg)

    fun currentBytes() = ByteArray(currentToken.size, { currentToken.pop() }).apply { reverse() }

    override fun nextToken(): Token {
        currentToken.clear()
        while (input.peek()?.isWhitespace() == true)
            input.take()
        return when (val c = input.take()) {
            null -> Token.EOF
            Bytes.LBRACKET -> Token.ListStart
            Bytes.RBRACKET -> Token.ListEnd
            Bytes.COLON -> Token.Colon
            Bytes.QUOTE -> parseString()
            Bytes.HYPHEN -> Token.Null
            else -> {
                // Accepts leading zeros.
                if (c.isDigit()) {
                    parseNumber(c)
                } else if (c.isIdStart()) {
                    parseBare(c)
                } else {
                    error("Unexpected character '$c'")
                }
            }
        }
    }

    override fun nextBytes(count: Int): ByteArray = ByteBuffer.allocate(count).apply {
        repeat(count) { nth ->
            put(input.take() ?: error("Unexpected EOF in byte $nth of run length encoded atom."))
        }
    }.array()

    // C style identifiers.
    private fun parseBare(init: Byte): Token {
        currentToken.push(init)
        while (input.peek()?.isIdPart() == true) {
            take()
        }
        return Token.LString(currentBytes())
    }

    // prefix to RLE atom.
    private fun parseNumber(init: Byte): Token.LInteger {
        currentToken.push(init)
        while (input.peek()?.isDigit() == true)
            take()
        val literal = currentBytes().string()
        return literal.toIntOrNull()?.let { lit -> Token.LInteger(lit) }
            ?: error("Invalid numeric literal: $literal")
    }

    // C style strings.
    private fun parseString(): Token {
        while (true) {
            when (val c = input.peek()) {
                null ->
                    error("Unexpected EOF in string literal.")

                Bytes.QUOTE -> {
                    input.take()
                    break
                }

                Bytes.ESCAPE -> {
                    input.take()
                    when (val e = input.take()) {
                        null -> error("Unexpected EOF in string literal.")
                        Bytes.ESCAPE -> currentToken.push(Bytes.ESCAPE)
                        Bytes.T -> currentToken.push(Bytes.TAB)
                        Bytes.R -> currentToken.push(Bytes.CARRIAGE_RETURN)
                        Bytes.N -> currentToken.push(Bytes.NEW_LINE)
                        Bytes.B -> currentToken.push(Bytes.BELL)
                        Bytes.QUOTE -> currentToken.push(Bytes.QUOTE)
                        else -> error("Invalid escape sequence: \\$e")
                    }
                }

                else if c.isStringUnescaped() ->
                    take()

                else ->
                    error("Invalid string literal character: ${"%02x".format(c)}")
            }
        }
        return Token.LString(currentBytes())
    }
}