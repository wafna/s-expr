package wafna.sexpr

/**
 * An s-expression is a tree of branch nodes (lists) and leaf nodes (atoms).
 * Atoms are arrays of bytes.
 */
sealed interface SExpr

/**
 * An empty atom (e.g. an empty string) is not the same as a null atom (e.g. an absent string).
 */
sealed class SAtom : SExpr
class SBytes(val data: ByteArray) : SAtom()
object SNull : SAtom()

class SList(val exprs: List<SExpr>) : SExpr

sealed class SExprError(msg: String, cause: Throwable? = null) : Exception(msg, cause) {
    // For syntactically invalid s-expressions.
    class Syntax(line: Int, column: Int, reason: String) : SExprError("ERROR ($line, $column): $reason")
    // For incorrectly structured s-expressions.
    class Type(msg: String, cause: Throwable? = null) : SExprError(msg, cause)
    // For the mapper.
    class Mapper(msg: String, cause: Throwable? = null) : SExprError(msg, cause)
}
