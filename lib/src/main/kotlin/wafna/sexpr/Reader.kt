package wafna.sexpr

/**
 * Translate the input into an s-expression.
 */
fun readSExpr(input: ByteStream): SList =
    TreeBuilder().apply { readSExpr(input, this) }.finish()

/**
 * Consume the parse output with the listener.
 */
fun readSExpr(input: ByteStream, listener: Listener) {
    val lexer = lexer(input)
    require(lexer.nextToken() == Token.LBracket)
    while (true) {
        when (val token = lexer.nextToken()) {
            Token.LBracket -> listener.startList()
            Token.RBracket -> if (listener.endList()) break
            is Token.LString -> listener.atom(SBytes(token.value))
            is Token.LInteger -> {
                require(Token.Colon == lexer.nextToken())
                val count = token.value
                val bytes = lexer.nextBytes(count)
                listener.atom(SBytes(bytes))
            }

            Token.Null -> listener.atom(SNull)
            Token.Colon -> error("Unexpected colon, ':'.")
            Token.EOF -> error("Missing required end of list, ']'.")
        }
    }
}

fun SExpr.requireList(msg: String = "Expected list."): SList = when (this) {
    is SList -> this
    else -> error(msg)
}

fun SExpr.requireAtom(msg: String = "Expected atom."): SAtom = when (this) {
    is SAtom -> this
    else -> error(msg)
}

fun SExpr.requireBytes(msg: String = "Expected bytes."): SBytes = when (this) {
    is SBytes -> this
    else -> error(msg)
}

fun <T> SExpr.mapAtom(msg: String = "Expecting non-null atom.", f: SBytes.() -> T) = when (this) {
    is  SBytes -> f(this)
    else -> error(msg)
}

fun ByteArray.string(): String = String(this, Charsets.UTF_8)
fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

/**
 * Convert atom data to UTF-8 string.
 */
fun SAtom.asString(): String? = when (this) {
    is SBytes -> data.string()
    is SNull -> null
}
