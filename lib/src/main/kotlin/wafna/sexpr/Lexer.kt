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
 * Create a Lexer for S-expressions.
 */
internal fun lexer(input: CharStream): Lexer = object : Lexer {
    val buffer = StringBuilder()
    override fun nextToken(): Token {
        buffer.clear()
        while (input.peek()?.isWhitespace() == true)
            input.take()
        return when (val c = input.take()) {
            null -> Token.EOF
            '[' -> Token.LBracket
            ']' -> Token.RBracket
            ':' -> Token.Colon
            '"' -> parseString()
            '-' -> parseNumber(c)
            else -> {
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

    private fun take() = input.take().also { buffer.append(it) }

    // Java identifiers.
    private fun parseBare(init: Char): Token {
        buffer.append(init)
        while (input.peek()?.isJavaIdentifierPart() == true) {
            take()
        }
        return Token.LString(buffer.toString())
    }

    private fun parseNumber(init: Char): Token.LInteger {
        buffer.append(init)
        while (input.peek()?.isDigit() == true) {
            take()
        }
        val literal = buffer.toString()
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
                        '\\' -> buffer.append('\\')
                        't' -> buffer.append('\t')
                        'r' -> buffer.append('\r')
                        'n' -> buffer.append('\n')
                        'b' -> buffer.append('\b')
                        '"' -> buffer.append('\"')
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
        return Token.LString(buffer.toString())
    }
}