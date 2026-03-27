package wafna.sexpr

/**
 * Input to the lexer.
 */
class ByteStream(private val iterator: Iterator<Byte>) {
    var column = 0
    var line = 0
    var peek: Byte? = null
    fun peek(): Byte? = if (null != peek) peek else {
        if (!iterator.hasNext()) null else iterator.next().also {
            peek = it
            if (it == Bytes.NEW_LINE) {
                column = 0
                ++line
            } else ++column
        }
    }

    fun take(): Byte? = if (null != peek) {
        peek.also { peek = null }
    } else {
        if (!iterator.hasNext()) null else iterator.next().also {
            if (it == Bytes.NEW_LINE) {
                column = 0
                ++line
            } else ++column
        }
    }

    companion object {
        fun from(s: String) = ByteStream(s.iterator().map { it.code.toByte() })
    }
}

fun <T, R> Iterator<T>.map(transform: (T) -> R): Iterator<R> = object : Iterator<R> {
    override fun next(): R = transform(this@map.next())
    override fun hasNext(): Boolean = this@map.hasNext()
}