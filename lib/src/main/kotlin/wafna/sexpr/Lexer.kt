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
    val currentToken = Stack<Byte>()
    fun take() {
        currentToken.push(input.take())
    }

    fun currentBytes() = ByteArray(currentToken.size, { currentToken.pop() }).apply { reverse() }

    override fun nextToken(): Token {
        currentToken.clear()
        while (input.peek()?.isWhitespace() == true)
            input.take()
        return when (val c = input.take()) {
            null -> Token.EOF
            Bytes.lbracket -> Token.LBracket
            Bytes.rbracket -> Token.RBracket
            Bytes.colon -> Token.Colon
            Bytes.quote -> parseString()
            Bytes.hyphen -> Token.Null
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
            put(
                input.take() ?: error("Unexpected EOF in byte $nth of run length encoded atom.")
            )
        }
    }.array()

    // Java identifiers.
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
        while (input.peek()?.isDigit() == true) {
            take()
        }
        val literal = currentBytes().string()
        return literal.toIntOrNull()?.let { lit -> Token.LInteger(lit) }
            ?: error("Invalid numeric literal: $literal")
    }

    // TODO incomplete: should support C strings exactly.
    private fun parseString(): Token {
        while (true) {
            when (val c = input.peek()) {
                null ->
                    error("Unexpected EOF in string literal.")

                Bytes.quote -> {
                    input.take()
                    break
                }

                Bytes.escape -> {
                    input.take()
                    when (val e = input.peek()) {
                        Bytes.escape -> currentToken.push(Bytes.escape)
                        Bytes.t -> currentToken.push(Bytes.tab)
                        Bytes.r -> currentToken.push(Bytes.carriageReturn)
                        Bytes.n -> currentToken.push(Bytes.newLine)
                        Bytes.b -> currentToken.push(Bytes.bell)
                        Bytes.quote -> currentToken.push(Bytes.quote)
                        else -> error("Invalid escape sequence: \\$e")
                    }
                    input.take()
                }

                else ->
                    // Ideally, only printable chars are left behind.
                    if (!c.isPrintable())
                        error("Invalid string literal character: ${"%02x".format(c)}")
                    else
                        take()
            }
        }
        return Token.LString(currentBytes())
    }
}