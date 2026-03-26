package wafna.sexpr

internal object Codes {
    const val lbracket = '['.code
    const val rbracket = ']'.code
    const val colon = ':'.code
    const val escape = '\\'.code
    const val newLine = '\n'.code
    const val carriageReturn = '\r'.code
    const val tab = '\t'.code
    const val bell = '\b'.code
    const val quote = '"'.code
    const val space = ' '.code
    const val hyphen = '-'.code
    const val n = 'n'.code
    const val r = 'r'.code
    const val t = 't'.code
    const val b = 'b'.code
    const val zero = '0'.code
}

internal object Bytes {
    const val lbracket = Codes.lbracket.toByte()
    const val rbracket = Codes.rbracket.toByte()
    const val colon = Codes.colon.toByte()
    const val escape = Codes.escape.toByte()
    const val newLine = Codes.newLine.toByte()
    const val carriageReturn = Codes.carriageReturn.toByte()
    const val tab = Codes.tab.toByte()
    const val bell = Codes.bell.toByte()
    const val quote = Codes.quote.toByte()
    const val space = Codes.space.toByte()
    const val hyphen = Codes.hyphen.toByte()
    const val n = Codes.n.toByte()
    const val r = Codes.r.toByte()
    const val t = Codes.t.toByte()
    const val b = Codes.b.toByte()
    const val zero = Codes.zero.toByte()
}

data class ByteAttrs(
    val printable: Boolean,
    val whitespace: Boolean,
    val digit: Boolean,
    val idStart: Boolean,
    val idPart: Boolean,
)

object ByteIs {
    val attrs = buildMap {
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

    fun printable(b: Byte) = attrs.getValue(b).printable
    fun whitespace(b: Byte) = attrs.getValue(b).whitespace
    fun digit(b: Byte) = attrs.getValue(b).digit
    fun idStart(b: Byte) = attrs.getValue(b).idStart
    fun idPart(b: Byte) = attrs.getValue(b).idPart
}
