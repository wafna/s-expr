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

private const val lbracket = '['.code
private const val rbracket = ']'.code
private const val escape = '\\'.code
private const val newLine = '\n'.code
private const val carriageReturn = '\r'.code
private const val tab = '\t'.code
private const val bell = '\b'.code
private const val quote = '"'.code
private const val space = ' '.code

private val CString = "\"([^\"\\\\]*|\\\\.)*\"".toRegex()

/**
 * Write an s-expression to an output stream.
 */
fun SExpr.write(stream: OutputStream, settings: WriterSettings.() -> Unit = {}): OutputStream {
    val settings = WriterSettings().apply(settings)
    fun writeBytes(data: ByteArray) {
        stream.write("${data.size}:".toByteArray(Charsets.UTF_8))
        stream.write(data)
    }

    fun writeCString(data: ByteArray) {
        stream.write(quote)
        data.forEach { byte ->
            when (val c = byte.toInt()) {
                escape -> with(stream) {
                    write(escape)
                    write(escape)
                }

                newLine -> with(stream) {
                    write(escape)
                    write('n'.code)
                }

                carriageReturn -> with(stream) {
                    write(escape)
                    write('r'.code)
                }

                tab -> with(stream) {
                    write(escape)
                    write('t'.code)
                }

                bell -> with(stream) {
                    write(escape)
                    write('b'.code)
                }

                quote -> with(stream) {
                    write(escape)
                    write(quote)
                }

                else -> stream.write(c)
            }
        }
        stream.write(quote)
    }

    fun node(s: SExpr, indent: Int) {
        fun doIndent() {
            if (settings.dataFormat != DataFormat.Canonical && null != settings.indent) {
                stream.write(newLine)
                repeat(indent) {
                    repeat(settings.indent!!) { stream.write(space) }
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
                stream.write(lbracket)
                s.exprs.forEachIndexed { i, e ->
                    if (settings.dataFormat != DataFormat.Canonical)
                        if (0 < i) stream.write(space)
                        else doIndent()
                    node(e, indent + 1)
                }
                doIndent()
                stream.write(rbracket)
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
    it.toByteArray().toString(Charsets.UTF_8)
}
