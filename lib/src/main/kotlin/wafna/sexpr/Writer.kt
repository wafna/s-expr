package wafna.sexpr

import java.io.OutputStream

enum class DataFormat {
    Bytes, CString, Bare
}

class WriterSettings {
    var indent: Int? = null
    var dataFormat: DataFormat = DataFormat.Bytes
}

private val lbracket = '['.code
private val rbracket = ']'.code
private val eol = '\n'.code
private val space = ' '.code

fun SExpr.write(stream: OutputStream, settings: WriterSettings.() -> Unit = {}): OutputStream {
    val settings = WriterSettings().apply(settings)
    fun node(s: SExpr, indent: Int) {
        when (s) {
            is SAtom -> when (settings.dataFormat) {
                DataFormat.Bytes -> {
                    stream.write("${s.data.size}:".toByteArray(Charsets.UTF_8))
                    stream.write(s.data)
                }

                DataFormat.CString -> TODO()
                DataFormat.Bare -> TODO()
            }

            is SList -> {
                if (null != settings.indent) {
                    stream.write(eol)
                    repeat(indent) {
                        repeat(settings.indent!!) { stream.write(space) }
                    }
                }
                stream.write(lbracket)
                s.exprs.forEach { node(it, indent + 1) }
                stream.write(rbracket)
            }
        }
    }
    node(this, 0)
    return stream
}