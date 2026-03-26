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

private val CString = "\"([^\"\\\\]*|\\\\.)*\"".toRegex()

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
        writeByte(Bytes.quote)
        data.forEach { byte ->
            when (val c = byte) {
                Bytes.escape -> with(stream) {
                    writeByte(Bytes.escape)
                    writeByte(Bytes.escape)
                }

                Bytes.newLine -> with(stream) {
                    writeByte(Bytes.escape)
                    write('n'.code)
                }

                Bytes.carriageReturn -> with(stream) {
                    writeByte(Bytes.escape)
                    write('r'.code)
                }

                Bytes.tab -> with(stream) {
                    writeByte(Bytes.escape)
                    write('t'.code)
                }

                Bytes.bell -> with(stream) {
                    writeByte(Bytes.escape)
                    write('b'.code)
                }

                Bytes.quote -> with(stream) {
                    writeByte(Bytes.escape)
                    writeByte(Bytes.quote)
                }

                else -> writeByte(c)
            }
        }
        writeByte(Bytes.quote)
    }

    fun node(s: SExpr, indent: Int) {
        fun doIndent() {
            if (settings.dataFormat != DataFormat.Canonical && null != settings.indent) {
                writeByte(Bytes.newLine)
                repeat(indent) {
                    repeat(settings.indent!!) { writeByte(Bytes.space) }
                }
            }
        }
        when (s) {
            is SNull -> stream.write('-'.code)
            is SBytes -> when (settings.dataFormat) {
                DataFormat.Canonical -> writeBytes(s.data)
                DataFormat.Readable -> if (s.data.first().toInt().toChar().isJavaIdentifierStart() && s.data.drop(1)
                        .all { it.toInt().toChar().isJavaIdentifierPart() }
                ) {
                    stream.write(s.data)
                } else if (s.data.none {
                        it.toInt().toChar().isISOControl()
                    }) writeCString(s.data) else writeBytes(s.data)
            }

            is SList -> {
                doIndent()
                writeByte(Bytes.lbracket)
                s.exprs.forEachIndexed { i, e ->
                    if (settings.dataFormat != DataFormat.Canonical)
                        if (0 < i) writeByte(Bytes.space)
                        else doIndent()
                    node(e, indent + 1)
                }
                doIndent()
                writeByte(Bytes.rbracket)
            }
        }
    }
    node(this, 0)
    return stream
}

/**
 * The canonical representation is RLE for all atoms and no whitespace.
 */
fun SExpr.showSExpr(settings: WriterSettings.() -> Unit = {}): String = ByteArrayOutputStream().use {
    write(it, settings)
    it.toByteArray().string()
}
