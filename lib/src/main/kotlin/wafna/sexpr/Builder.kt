package wafna.sexpr

import java.util.*

class SExprBuilder {
    private val exprs = Stack<SExpr>()
    /**
     * Add bytes as an atom.
     */
    fun atom(bytes: ByteArray) {
        exprs.push(SAtom(bytes))
    }

    /**
     * Add string as an atom.
     */
    fun atom(string: String) {
        exprs.push(SAtom(string.toByteArray()))
    }

    fun empty() {
        exprs.push(SAtom.NULL)
    }

    fun list(f: SExprBuilder.() -> Unit) {
        val builder = SExprBuilder()
        builder.f()
        exprs.push(builder.result())
    }

    fun any(expr: SExpr) {
        exprs.push(expr)
    }

    internal fun result(): SList = SList(exprs.take(exprs.size))

}

/**
 * Declare literal s-expressions
 */
fun buildSExpr(f: SExprBuilder.() -> Unit): SList = SExprBuilder().run {
    f()
    result()
}