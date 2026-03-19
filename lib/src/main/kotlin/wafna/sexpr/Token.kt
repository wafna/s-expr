package wafna.sexpr

sealed interface Token {
    object EOF : Token
    object LBracket : Token
    object RBracket : Token
    object Colon : Token
    // Literals
    class LInt(val value: Int) : Token
    class LDouble(val value: Double) : Token
    class LString(val value: String) : Token
    class LBytes(val value: ByteArray) : Token
}