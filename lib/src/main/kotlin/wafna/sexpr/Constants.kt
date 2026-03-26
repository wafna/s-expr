package wafna.sexpr

internal object Bytes {
    const val lbracket = '['.code.toByte()
    const val rbracket = ']'.code.toByte()
    const val colon = ':'.code.toByte()
    const val quote = '"'.code.toByte()
    const val space = ' '.code.toByte()
    const val hyphen = '-'.code.toByte()
    const val escape = '\\'.code.toByte()
    const val newLine = '\n'.code.toByte()
    const val carriageReturn = '\r'.code.toByte()
    const val tab = '\t'.code.toByte()
    const val bell = '\b'.code.toByte()
    const val n = 'n'.code.toByte()
    const val r = 'r'.code.toByte()
    const val t = 't'.code.toByte()
    const val b = 'b'.code.toByte()
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
