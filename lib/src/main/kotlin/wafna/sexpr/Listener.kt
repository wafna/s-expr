package wafna.sexpr

import java.util.*

/**
 * Listens to events from the parser, thus "reading" the tokenized input.
 */
interface Listener {
    fun atom(e: SAtom)
    fun startList()
    /**
     * Signals whether the expression is complete.
     */
    fun endList(): Boolean
    /**
     * Signals whether the expression is complete.
     */
    fun list(f: () -> Unit): Boolean {
        startList()
        f()
        return endList()
    }
}

/**
 * Builds the full expression tree from the parser.
 */
class TreeBuilder : Listener {
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

    fun finish(): SExpr = exprs.pop().also {
        require(exprs.isEmpty()) {
            "Malformed expression: unclosed list(s)."
        }
    }
}
