package wafna.sexpr

sealed interface Token {
    object EOF : Token
    object LBracket : Token
    object RBracket : Token
    object Colon : Token
    // Literals
    data class LString(val value: String) : Token
    data class LInteger(val value: Int) : Token
}