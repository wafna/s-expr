package wafna.sexpr

/**
 * Special values used throughout the code.
 */
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
private data class ByteAttrs(
    val isWhitespace: Boolean,
    val isDigit: Boolean,
    val isIdStart: Boolean,
    val isIdPart: Boolean,
    val isStringUnescaped: Boolean,
    val isEscapable: Boolean,
)

private val punctuation = buildSet { "!@#$%^&*()[]{}/?=+|-_',.".forEach { add(it) } }
private val escapes = buildSet { "\r\n\t\b".forEach { add(it) } }

private val attrs = buildMap {
    (Byte.MIN_VALUE..Byte.MAX_VALUE).forEach { byte ->
        val c = byte.toChar()
        put(
            byte.toByte(), ByteAttrs(
                isWhitespace = c.isWhitespace(),
                isDigit = c.isDigit(),
                isIdStart = c.isJavaIdentifierStart(),
                isIdPart = c.isJavaIdentifierPart(),
                isStringUnescaped = c.isLetterOrDigit() || c == ' ' || punctuation.contains(c),
                isEscapable = escapes.contains(c),
            )
        )
    }
}

// Attribute methods.

fun Byte.isWhitespace(): Boolean = attrs.getValue(this).isWhitespace
fun Byte.isDigit(): Boolean = attrs.getValue(this).isDigit
fun Byte.isIdStart(): Boolean = attrs.getValue(this).isIdStart
fun Byte.isIdPart(): Boolean = attrs.getValue(this).isIdPart
fun Byte.isStringUnescaped(): Boolean = attrs.getValue(this).isStringUnescaped
fun Byte.isEscapable(): Boolean = attrs.getValue(this).isEscapable