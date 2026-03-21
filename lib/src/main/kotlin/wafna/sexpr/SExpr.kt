package wafna.sexpr

/**
 * An s-expression is a tree of branch nodes (lists) and leaf nodes (atoms).
 * Atoms are arrays of bytes.
 */
sealed class SExpr
class SAtom(val data: ByteArray) : SExpr()
class SList(val exprs: List<SExpr>) : SExpr()

