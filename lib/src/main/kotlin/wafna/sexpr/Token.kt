package wafna.sexpr

sealed interface Token {
    object EOF : Token
    object LBracket : Token
    object RBracket : Token
    object Colon : Token
    // Literals
    data class LInteger(val value: Long) : Token
    data class LDouble(val value: Double) : Token
    data class LString(val value: String) : Token
    class LBytes(val value: ByteArray) : Token
}