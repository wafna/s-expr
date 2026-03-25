package wafna.sexpr

import java.util.*

/**
 * Listens to events from the parser, thus "reading" the tokenized input.
 */
interface Reader {
    fun atom(e: SAtom)
    fun startList()
    /**
     * Signals whether the expression is complete.
     */
    fun endList(): Boolean
}

/**
 * Builds the full expression tree from the parser.
 */
internal class TreeBuilder : Reader {
    val exprs = Stack<SExpr>()
    val sizes = Stack<Int>().apply { push(0) }
    override fun atom(e: SAtom) {
        exprs.push(e)
    }

    override fun startList() {
        sizes.push(exprs.size)
    }

    override fun endList(): Boolean {
        val size = exprs.size - sizes.pop()
        val nodes = List(size) { exprs.pop() }.reversed()
        exprs.push(SList(nodes))
        return sizes.isEmpty()
    }

    fun finish(): SList = exprs.pop().also {
        require(exprs.isEmpty()) { "Malformed expression: unclosed list(s)." }
    }.requireList()
}

/**
 * Translate the input into an s-expression.
 */
fun readSExpr(input: CharStream): SList =
    TreeBuilder().apply { readSExpr(input, this) }.finish()

/**
 * Consume the parse output with the reader.
 */
fun readSExpr(input: CharStream, reader: Reader) {
    val lexer = lexer(input)
    require(lexer.nextToken() == Token.LBracket)
    while (true) {
        when (val token = lexer.nextToken()) {
            Token.LBracket -> reader.startList()
            Token.RBracket -> if (reader.endList()) break
            is Token.LString -> reader.atom(SBytes(token.value.toByteArray(Charsets.UTF_8)))
            is Token.LInteger -> {
                require(Token.Colon == lexer.nextToken())
                val count = token.value
                val bytes = lexer.nextBytes(count)
                reader.atom(SBytes(bytes))
            }

            Token.Null -> reader.atom(SNull)
            Token.Colon -> error("Unexpected colon, ':'.")
            Token.EOF -> error("Missing required end of list, ']'.")
        }
    }
}

fun SExpr.requireList(msg: String = "Expected list."): SList = when (this) {
    is SAtom -> error(msg)
    is SList -> this
}

fun SExpr.requireAtom(msg: String = "Expected atom."): SAtom = when (this) {
    is SAtom -> this
    is SList -> error(msg)
}

fun <T> SExpr.mapAtom(msg: String = "Expecting non-null atom.", f: SBytes.() -> T) = when (this) {
    is SAtom -> when (this) {
        is SNull -> null
        is SBytes -> f(this)
    }

    is SList -> error(msg)
}

/**
 * Convert atom data to UTF-8 string.
 */
fun SAtom.asString(): String? = when (this) {
    is SBytes -> String(data, Charsets.UTF_8)
    is SNull -> null
}
