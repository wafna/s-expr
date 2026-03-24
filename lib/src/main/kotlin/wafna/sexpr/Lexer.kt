package wafna.sexpr

import java.nio.ByteBuffer

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
internal fun lexer(input: CharStream): Lexer = object : Lexer {
    val currentToken = StringBuilder()
    fun take() {
        currentToken.append(input.take())
    }

    override fun nextToken(): Token {
        currentToken.clear()
        while (input.peek()?.isWhitespace() == true)
            input.take()
        return when (val c = input.take()) {
            null -> Token.EOF
            '[' -> Token.LBracket
            ']' -> Token.RBracket
            ':' -> Token.Colon
            '"' -> parseString()
            else -> {
                // Accepts leading zeros.
                if (c.isDigit()) {
                    parseNumber(c)
                } else if (c.isJavaIdentifierStart()) {
                    parseBare(c)
                } else {
                    error("Unexpected character '$c'")
                }
            }
        }
    }

    override fun nextBytes(count: Int): ByteArray = ByteBuffer.allocate(count).apply {
        repeat(count) { nth ->
            put(
                input.take()?.code?.toByte() ?: error("Unexpected EOF in byte $nth of run length encoded atom.")
            )
        }
    }.array()

    // Java identifiers.
    private fun parseBare(init: Char): Token {
        currentToken.append(init)
        while (input.peek()?.isJavaIdentifierPart() == true) {
            take()
        }
        return Token.LString(currentToken.toString())
    }

    // prefix to RLE atom.
    private fun parseNumber(init: Char): Token.LInteger {
        currentToken.append(init)
        while (input.peek()?.isDigit() == true) {
            take()
        }
        val literal = currentToken.toString()
        return literal.toIntOrNull()?.let { lit -> Token.LInteger(lit) }
            ?: error("Invalid numeric literal: $literal")
    }

    // TODO incomplete: should support C strings exactly.
    private fun parseString(): Token {
        while (true) {
            when (val c = input.peek()) {
                null ->
                    error("Unexpected EOF in string literal.")

                '"' -> {
                    input.take()
                    break
                }

                '\\' -> {
                    input.take()
                    when (val e = input.peek()) {
                        '\\' -> currentToken.append('\\')
                        't' -> currentToken.append('\t')
                        'r' -> currentToken.append('\r')
                        'n' -> currentToken.append('\n')
                        'b' -> currentToken.append('\b')
                        '"' -> currentToken.append('\"')
                        else -> error("Invalid escape sequence: \\$e")
                    }
                    input.take()
                }

                else ->
                    // Ideally, only printable chars are left behind.
                    if (c.isISOControl())
                        error("Invalid control character: ${"%02x".format(c.code.toByte())}")
                    else
                        take()
            }
        }
        return Token.LString(currentToken.toString())
    }
}