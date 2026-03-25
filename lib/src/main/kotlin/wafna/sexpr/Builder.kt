package wafna.sexpr

import java.util.*

class SExprBuilder {
    private val exprs = Stack<SExpr>()
    /**
     * Add bytes as an atom.
     */
    fun atom(bytes: ByteArray) {
        exprs.push(SBytes(bytes))
    }

    /**
     * Add string as an atom.
     */
    fun atom(string: String) {
        exprs.push(SBytes(string.toByteArray()))
    }

    /**
     * Create a list.
     */
    fun list(f: SExprBuilder.() -> Unit) {
        val builder = SExprBuilder()
        builder.f()
        exprs.push(builder.result())
    }

    /**
     * Add anything to the current list.
     */
    fun expr(expr: SExpr) {
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