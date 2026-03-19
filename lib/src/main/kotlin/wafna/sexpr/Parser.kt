package wafna.sexpr

fun lexer(input: CharStream): Iterator<Token> = object : Iterator<Token> {
    val buffer = StringBuilder()
    var eof = false
    override fun next(): Token {
        buffer.clear()
        return if (eof) Token.EOF
        else {
            when (val c = input.peek()) {
                null -> {
                    eof = true
                    Token.EOF
                }
                '[' -> Token.LBracket
                ']' -> Token.RBracket
                else -> {
                    if (c.isDigit()) {

                    }
                    TODO()
                }
            }
        }
    }

    override fun hasNext(): Boolean = ! eof
}
