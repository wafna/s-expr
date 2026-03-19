package wafna.sexpr

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class TestParser {
    @Test
    fun test() {
        parse("[]".toCharStream()).apply {
            assertEquals(exprs.size, 0)
        }
        parse("[a]".toCharStream()).apply {
            assertEquals(exprs.size, 1)
            assertAtom(exprs[0], "a")
        }
        parse("[a bc]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            assertAtom(exprs[1], "bc")
        }
        parse("[a [b c]]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            (exprs[1] as SList).apply {
                assertEquals(exprs.size, 2)
                assertAtom(exprs[0], "b")
                assertAtom(exprs[1], "c")
            }
        }
        parse("[1:a [1:b 1:c]]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0], "a")
            (exprs[1] as SList).apply {
                assertEquals(exprs.size, 2)
                assertAtom(exprs[0], "b")
                assertAtom(exprs[1], "c")
            }
        }
    }
}