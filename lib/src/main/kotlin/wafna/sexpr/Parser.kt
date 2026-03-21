package wafna.sexpr

import java.util.*

/**
 * Translate the input into an s-expression.
 */
fun parse(input: CharStream): SList {
    val exprs = Stack<SExpr>()
    val sizes = Stack<Int>().apply { push(0) }
    val lexer = lexer(input)
    require(lexer.nextToken() == Token.LBracket)
    while (true) {
        when (val token = lexer.nextToken()) {
            Token.LBracket ->
                sizes.push(exprs.size)

            Token.RBracket -> {
                val size = exprs.size - sizes.pop()
                val nodes = List(size) { exprs.pop() }.reversed()
                exprs.push(SList(nodes))
                if (sizes.isEmpty())
                    break // All lists are closed.
            }

            is Token.LString ->
                exprs.push(SAtom(token.value.toByteArray(Charsets.UTF_8)))

            is Token.LInteger -> {
                require(Token.Colon == lexer.nextToken())
                val count = token.value
                val bytes = lexer.nextBytes(count)
                exprs.push(SAtom(bytes))
            }

            Token.Colon -> error("Unexpected colon, ':'.")
            Token.EOF -> error("Missing required end of list, ']'.")
        }
    }
    return (exprs.pop() as SList).also {
        require(exprs.isEmpty()) { "Malformed expression: unclosed list(s)." }
    }
}
