package wafna.sexpr

import java.util.Stack

fun parse(input: CharStream): SList {
    val lexer = lexer(input)
    val exprs = Stack<SExpr>()
    val sizes = Stack<Int>()
    require(lexer.nextToken() == Token.LBracket)
    sizes.push(0)
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
    require(Token.EOF == lexer.nextToken()) { "Input not completely consumed."}
    require(1 == exprs.size)
    return exprs.pop() as SList
}
