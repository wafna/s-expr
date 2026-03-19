package wafna.sexpr

import java.nio.ByteBuffer

interface Lexer {
    fun nextToken(): Token
    fun nextBytes(count: Int): ByteArray
}

fun lexer(input: CharStream): Lexer = object : Lexer {
    val buffer = StringBuilder()
    var eof = false
    override fun nextToken(): Token {
        buffer.clear()
        while (input.peek()?.isWhitespace() == true)
            input.take()
        return if (eof) Token.EOF
        else {
            println(input.peek())
            when (val c = input.peek()) {
                null -> {
                    eof = true
                    Token.EOF
                }

                '[' -> {
                    grab(); Token.LBracket
                }

                ']' -> {
                    grab(); Token.RBracket
                }

                ':' -> {
                    grab(); Token.Colon
                }

                '"' -> parseString()
                '-' -> parseNumber()
                else -> {
                    if (c.isDigit()) {
                        parseNumber()
                    } else if (c.isJavaIdentifierStart()) {
                        parseBare()
                    } else {
                        error("")
                    }
                }
            }
        }
    }

    override fun nextBytes(count: Int): ByteArray = ByteBuffer.allocate(count).apply {
        repeat(count) { put(input.take()?.code?.toByte() ?: 0) }
    }.array()

    private fun grab() = buffer.append(input.take())
    private fun parseBare(): Token {
        grab()
        while (input.peek()?.isJavaIdentifierPart() == true) {
            grab()
        }
        return Token.LString(buffer.toString())
    }

    private fun parseNumber(): Token {
        grab()
        while (true) {
            // Grab anything that could be part of a numeric literal.
            when (val c = input.peek()) {
                null -> break
                '-' -> grab()
                'e' -> grab()
                'E' -> grab()
                '.' -> grab()
                else -> if (c.isDigit()) grab() else break
            }
        }
        val literal = buffer.toString()
        return literal.toDoubleOrNull()?.let { lit -> Token.LDouble(lit) }
            ?: literal.toIntOrNull()?.let { Token.LInt(it) }
            ?: error("Invalid numeric literal: $literal")
    }

    private fun parseString(): Token {
        input.take() // discard
        while (true) {
            when (val c = input.peek()) {
                null -> error("Unexpected EOF in string literal.")
                '"' -> break
                '\\' -> {
                    input.take() // discard
                    when (val e = input.take()) {
                        '\\' -> buffer.append('\\')
                        '\t' -> buffer.append('\t')
                        '\r' -> buffer.append('\r')
                        '\n' -> buffer.append('\n')
                        '\b' -> buffer.append('\b')
                        '\"' -> buffer.append('\"')
                        else -> error("Invalid escape sequence: \\$e")
                    }
                }

                else ->
                    // Ideally, only printable chars are left behind.
                    if (c.isISOControl())
                        error("Invalid control character: ${"%02x".format(c.code.toByte())}")
                    else grab()
            }
        }
        return Token.LString(buffer.toString())
    }
}