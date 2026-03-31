package wafna.sexpr

import java.io.OutputStream
import java.util.*

/**
 * Listens to events from the parser, thus "reading" the tokenized input.
 */
abstract class Listener {
    abstract fun atom(e: SAtom)
    internal abstract fun startList()
    internal abstract fun endList()
    fun atom(bytes: ByteArray) = atom(SBytes(bytes))
    fun atom() = atom(SNull)
    fun list(f: () -> Unit) {
        startList()
        f()
        endList()
    }
}

/**
 * Builds the full expression tree from the parser.
 */
class TreeBuilder : Listener() {
    val exprs = Stack<SExpr>()
    val sizes = Stack<Int>().apply { push(0) }
    override fun atom(e: SAtom) {
        exprs.push(e)
    }

    override fun startList() {
        sizes.push(exprs.size)
    }

    override fun endList() {
        val size = exprs.size - sizes.pop()
        val nodes = List(size) { exprs.pop() }.reversed()
        exprs.push(SList(nodes))
        sizes.isEmpty()
    }

    fun finish(): SExpr = exprs.pop().also {
        if(!exprs.isEmpty()) {
            throw SExprError.EOF("unclosed list(s).")
        }
    }
}

class StreamSink(private val stream: OutputStream) : Listener() {
    override fun atom(e: SAtom) {
        when (e) {
            is SNull -> stream.write(Bytes.HYPHEN.toInt())
            is SBytes -> {
                stream.write(e.data.size.toString().bytes())
                stream.write(Bytes.COLON.toInt())
                stream.write(e.data)
            }
        }
    }

    override fun startList() {
        stream.write(Bytes.LBRACKET.toInt())
    }

    override fun endList() {
        stream.write(Bytes.RBRACKET.toInt())
    }
}
