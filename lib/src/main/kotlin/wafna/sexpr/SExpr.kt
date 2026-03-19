package wafna.sexpr

sealed class SExpr()
class SAtom(val data: ByteArray) : SExpr()
class SList(val exprs: List<SExpr>) : SExpr()

