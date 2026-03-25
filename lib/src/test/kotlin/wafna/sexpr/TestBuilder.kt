package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals

class TestBuilder {
    @Test
    fun test() {
        buildSExpr {
            atom("a")
            atom("b")
        }.run {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            assertAtom(exprs[1], "b")
        }
        buildSExpr {
            atom("a")
            list {
                atom("b")
                atom("c".bytes())
            }
        }.run {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            (exprs[1] as SList).run {
                assertEquals(exprs.size, 2)
                assertAtom(exprs[0], "b")
                assertAtom(exprs[1], "c")
            }
        }
    }
}