package wafna.sexpr

/**
 * An s-expression is a tree of branch nodes (lists) and leaf nodes (atoms).
 * Atoms are arrays of bytes.
 */
sealed interface SExpr
/**
 * Atoms contain lists of bytes or are null.
 */
sealed class SAtom : SExpr
class SBytes(val data: ByteArray) : SAtom()
object SNull : SAtom()
/**
 * Lists contain other s-expressions (in order).
 */
class SList(val exprs: List<SExpr>) : SExpr

sealed class SExprError(msg: String, cause: Throwable? = null) : Exception(msg, cause) {
    // For syntactically invalid s-expressions.
    sealed class Syntax(msg: String, cause: Throwable? = null) : Exception(msg, cause)
    class Token(line: Int, column: Int, reason: String) : Syntax("Syntax error ($line, $column): $reason")
    class EOF(reason: String) : Syntax("Unexpected end of input: $reason")
    // For incorrectly structured s-expressions.
    class Type(msg: String, cause: Throwable? = null) : SExprError(msg, cause)
    // For mapper errors.
    class Mapper(msg: String, cause: Throwable? = null) : SExprError(msg, cause)
}
