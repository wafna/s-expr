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
