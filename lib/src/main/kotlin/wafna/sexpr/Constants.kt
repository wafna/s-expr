package wafna.sexpr

internal object Bytes {
    const val LBRACKET = '['.code.toByte()
    const val RBRACKET = ']'.code.toByte()
    const val COLON = ':'.code.toByte()
    const val QUOTE = '"'.code.toByte()
    const val SPACE = ' '.code.toByte()
    const val HYPHEN = '-'.code.toByte()
    const val ESCAPE = '\\'.code.toByte()
    const val NEW_LINE = '\n'.code.toByte()
    const val CARRIAGE_RETURN = '\r'.code.toByte()
    const val TAB = '\t'.code.toByte()
    const val BELL = '\b'.code.toByte()
    const val N = 'n'.code.toByte()
    const val R = 'r'.code.toByte()
    const val T = 't'.code.toByte()
    const val B = 'b'.code.toByte()
}

data class ByteAttrs(
    val printable: Boolean,
    val whitespace: Boolean,
    val digit: Boolean,
    val idStart: Boolean,
    val idPart: Boolean,
)

private val attrs = buildMap {
    (Byte.MIN_VALUE..Byte.MAX_VALUE).forEach { byte ->
        val c = byte.toChar()
        put(
            byte.toByte(), ByteAttrs(
                printable = !c.isISOControl(), // good enough?
                whitespace = c.isWhitespace(),
                digit = c.isDigit(),
                idStart = c.isJavaIdentifierStart(),
                idPart = c.isJavaIdentifierPart()
            )
        )
    }
}

fun Byte.isPrintable(): Boolean = attrs.getValue(this).printable
fun Byte.isWhitespace(): Boolean = attrs.getValue(this).whitespace
fun Byte.isDigit(): Boolean = attrs.getValue(this).digit
fun Byte.isIdStart(): Boolean = attrs.getValue(this).idStart
fun Byte.isIdPart(): Boolean = attrs.getValue(this).idPart
