package wafna.sexpr

import kotlin.test.assertEquals
import kotlin.test.fail

fun parse(input: String) = readSExpr(CharStream.from(input))

fun assertAtom(expected: SExpr, actual: String) {
    val expected = expected as SBytes
    assertEquals(String(expected.data), actual)
}

fun compareSExpr(actual: SExpr, expected: SExpr) {
    when (actual) {
        SNull -> when (expected) {
            SNull -> {}
            else -> fail("Expected $expected to be null")
        }
        is SBytes -> when (expected) {
            is SBytes -> assertEquals(expected.data.toString(Charsets.UTF_8), actual.data.toString(Charsets.UTF_8))
            else -> fail("Incompatible: expected SAtom, got ${expected::class.simpleName}")
        }

        is SList -> when (expected) {
            is SList -> {
                assertEquals(expected.exprs.size, actual.exprs.size)
                expected.exprs.zip(actual.exprs).forEach { (a, b) ->
                    compareSExpr(a, b)
                }
            }

            else -> fail("Incompatible: expected SAtom, got ${expected::class.simpleName}")
        }
    }
}

