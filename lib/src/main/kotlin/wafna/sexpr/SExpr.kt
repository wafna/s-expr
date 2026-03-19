package wafna.sexpr

sealed class SExpr()
class SAtom(data: ByteArray) : SExpr()
class SList(exprs: List<SExpr>) : SExpr()

