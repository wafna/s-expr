package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals

class TestBuilder {
    @Test
    fun test() {
        buildSExpr {
            +"a"
            +"b"
        }.run {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            assertAtom(exprs[1], "b")
        }
        buildSExpr {
            +"a"
            list {
                +"b"
                +"c".toByteArray()
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