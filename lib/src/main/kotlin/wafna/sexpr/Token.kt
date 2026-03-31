package wafna.sexpr

internal sealed interface Token {
    object EOF : Token
    object ListStart : Token
    object ListEnd : Token
    object Colon : Token
    object Null : Token
    // Literals
    class LString(val value: ByteArray) : Token
    data class LInteger(val value: Int) : Token
}