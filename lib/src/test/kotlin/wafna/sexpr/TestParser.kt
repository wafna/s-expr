package wafna.sexpr

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

fun assertAtom(expected: SAtom, actual: String) {
    assertEquals(String(expected.data), actual)
}

class TestParser {
    @Test
    fun test() {
        parse("[]".toCharStream()).apply {
            assertEquals(exprs.size, 0)
        }
        parse("[a]".toCharStream()).apply {
            assertEquals(exprs.size, 1)
            assertAtom(exprs[0] as SAtom, "a")
        }
        parse("[a bc]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0] as SAtom, "a")
            assertAtom(exprs[1] as SAtom, "bc")
        }
        parse("[a [b c]]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0] as SAtom, "a")
            (exprs[1] as SList).apply {
                assertEquals(exprs.size, 2)
                assertAtom(exprs[0] as SAtom, "b")
                assertAtom(exprs[1] as SAtom, "c")
            }
        }
        parse("[1:a [1:b 1:c]]".toCharStream()).apply {
            assertEquals(exprs.size, 2)
            assertAtom(exprs[0] as SAtom, "a")
            (exprs[1] as SList).apply {
                assertEquals(exprs.size, 2)
                assertAtom(exprs[0] as SAtom, "b")
                assertAtom(exprs[1] as SAtom, "c")
            }
        }
    }
}