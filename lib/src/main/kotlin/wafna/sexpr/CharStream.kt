package wafna.sexpr

class CharStream(private val iterator: Iterator<Char>) {
    var column = 0
    var line = 0
    var peek: Char? = null
    fun peek(): Char? = if (null != peek) peek else {
        if (!iterator.hasNext()) null else iterator.next().also {
            peek = it
            if (it == '\n') {
                column = 0
                ++line
            } else ++column
        }
    }
    fun take(): Char? = if (null != peek) {
        peek.also { peek = null }
    } else {
        if (!iterator.hasNext()) null else iterator.next().also {
            if (it == '\n') {
                column = 0
                ++line
            } else ++column
        }
    }
}
