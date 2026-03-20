package wafna.sexpr

import java.util.*

fun parse(input: CharStream): SList {
    val exprs = Stack<SExpr>()
    val sizes = Stack<Int>().apply { push(0) }
    val lexer = lexer(input)
    require(lexer.nextToken() == Token.LBracket)
    while (true) {
        when (val token = lexer.nextToken()) {
            Token.LBracket -> {
                sizes.push(exprs.size)
            }

            Token.RBracket -> {
                val size = exprs.size - sizes.pop()
                val nodes = List(size) { exprs.pop() }.reversed()
                exprs.push(SList(nodes))
                if (sizes.isEmpty())
                    break
            }

            is Token.LString -> {
                exprs.push(SAtom(token.value.toByteArray(Charsets.UTF_8)))
            }

            is Token.LInteger -> {
                require(Token.Colon == lexer.nextToken())
                val count = token.value
                val bytes = lexer.nextBytes(count)
                exprs.push(SAtom(bytes))
            }

            Token.Colon -> error("Colon outside of RLE atom.")
            Token.EOF -> error("Missing required end of list.")
        }
    }
//    require(Token.EOF == lexer.nextToken()) { "Input not completely consumed." }
    return exprs.pop().also { require(exprs.isEmpty()) } as SList
}
