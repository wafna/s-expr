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

/**
 * Classifications of bytes for reading and writing.
 */
data class ByteAttrs(
    val whitespace: Boolean,
    val digit: Boolean,
    val idStart: Boolean,
    val idPart: Boolean,
    val stringUnescaped: Boolean,
    val escapable: Boolean,
)

private val punctuation = buildSet { "!@#$%^&*()[]{}/?=+|-_',.".forEach { add(it) } }
private val escapes = buildSet { "\r\n\t\b".forEach { add(it) } }

private val attrs = buildMap {
    (Byte.MIN_VALUE..Byte.MAX_VALUE).forEach { byte ->
        val c = byte.toChar()
        put(
            byte.toByte(), ByteAttrs(
                whitespace = c.isWhitespace(),
                digit = c.isDigit(),
                idStart = c.isJavaIdentifierStart(),
                idPart = c.isJavaIdentifierPart(),
                stringUnescaped = c.isLetterOrDigit() || c == ' ' || punctuation.contains(c),
                escapable = escapes.contains(c),
            )
        )
    }
}

fun Byte.isWhitespace(): Boolean = attrs.getValue(this).whitespace
fun Byte.isDigit(): Boolean = attrs.getValue(this).digit
fun Byte.isIdStart(): Boolean = attrs.getValue(this).idStart
fun Byte.isIdPart(): Boolean = attrs.getValue(this).idPart
fun Byte.isStringUnescaped(): Boolean = attrs.getValue(this).stringUnescaped
fun Byte.isEscapable(): Boolean = attrs.getValue(this).escapable