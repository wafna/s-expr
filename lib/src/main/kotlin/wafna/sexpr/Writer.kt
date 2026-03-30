package wafna.sexpr

import java.io.ByteArrayOutputStream
import java.io.OutputStream

enum class DataFormat {
    Canonical, Readable
}

class WriterSettings {
    var indent: Int? = null
    var dataFormat: DataFormat = DataFormat.Canonical
}

/**
 * Write an s-expression to an output stream.
 */
fun SExpr.write(stream: OutputStream, settings: WriterSettings.() -> Unit = {}): OutputStream {
    val settings = WriterSettings().apply(settings)
    fun writeBytes(data: ByteArray) {
        stream.write("${data.size}:".bytes())
        stream.write(data)
    }

    fun writeByte(data: Byte) {
        stream.write(data.toInt())
    }

    fun writeCString(data: ByteArray) {
        writeByte(Bytes.QUOTE)
        data.forEach { byte ->
            when (val c = byte) {
                Bytes.ESCAPE -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.ESCAPE)
                }

                Bytes.NEW_LINE -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.N)
                }

                Bytes.CARRIAGE_RETURN -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.R)
                }

                Bytes.TAB -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.T)
                }

                Bytes.BELL -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.B)
                }

                Bytes.QUOTE -> with(stream) {
                    writeByte(Bytes.ESCAPE)
                    writeByte(Bytes.QUOTE)
                }

                else -> writeByte(c)
            }
        }
        writeByte(Bytes.QUOTE)
    }

    fun node(s: SExpr, indent: Int, top: Boolean = false) {
        fun doIndent() {
            if (settings.dataFormat != DataFormat.Canonical && null != settings.indent) {
                writeByte(Bytes.NEW_LINE)
                repeat(indent) {
                    repeat(settings.indent!!) { writeByte(Bytes.SPACE) }
                }
            }
        }
        when (s) {
            is SNull -> writeByte(Bytes.HYPHEN)
            is SBytes -> when (settings.dataFormat) {
                DataFormat.Canonical -> writeBytes(s.data)
                DataFormat.Readable -> if (s.data.first().toInt().toChar().isJavaIdentifierStart() && s.data.drop(1)
                        .all { it.toInt().toChar().isJavaIdentifierPart() }
                ) {
                    stream.write(s.data)
                } else if (s.data.all { it.isStringUnescaped() || it.isEscapable() }) {
                    writeCString(s.data)
                } else {
                    writeBytes(s.data)
                }
            }

            is SList -> {
                if (!top) doIndent()
                writeByte(Bytes.LBRACKET)
                s.exprs.forEachIndexed { i, e ->
                    if (settings.dataFormat != DataFormat.Canonical)
                        when (e) {
                            is SAtom -> if (0 < i) writeByte(Bytes.SPACE)
                            else -> {}
                        }
                    node(e, indent + 1)
                }
                writeByte(Bytes.RBRACKET)
            }
        }
    }
    node(this, 0, true)
    return stream
}

/**
 * The canonical representation is RLE for all atoms and no whitespace.
 */
fun SExpr.showSExpr(settings: WriterSettings.() -> Unit = {}): String = ByteArrayOutputStream().use {
    write(it, settings)
    it.toByteArray().string()
}
