package wafna.sexpr

import java.util.*

class SExprBuilder {
    private val exprs = Stack<SExpr>()
    fun atom(bytes: ByteArray) {
        exprs.push(SAtom(bytes))
    }

    operator fun String.unaryPlus() = atom(toByteArray(Charsets.UTF_8))
    operator fun ByteArray.unaryPlus() = atom(this)

    fun list(f: SExprBuilder.() -> Unit) {
        val builder = SExprBuilder()
        builder.f()
        exprs.push(builder.result())
    }

    fun result(): SList = SList(exprs.take(exprs.size))
}

fun buildSExpr(f: SExprBuilder.() -> Unit): SList = SExprBuilder().run {
    f()
    result()
}