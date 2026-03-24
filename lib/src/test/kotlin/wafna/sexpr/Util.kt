package wafna.sexpr

import kotlin.test.assertEquals
import kotlin.test.fail

fun parse(input: String) = readSExpr(CharStream.from(input))

fun assertAtom(expected: SExpr, actual: String) {
    val expected = expected as SAtom
    assertEquals(String(expected.data), actual)
}

fun compareSExpr(actual: SExpr, expected: SExpr) {
    when (actual) {
        is SAtom -> when (expected) {
            is SAtom -> assertEquals(expected.data.toString(Charsets.UTF_8), actual.data.toString(Charsets.UTF_8))
            is SList -> fail("Incompatible: expected SList, got SAtom")
        }

        is SList -> when (expected) {
            is SList -> {
                assertEquals(expected.exprs.size, actual.exprs.size)
                expected.exprs.zip(actual.exprs).forEach { (a, b) ->
                    compareSExpr(a, b)
                }
            }

            is SAtom -> fail("Incompatible: expected SAtom, got SList")
        }
    }
}

