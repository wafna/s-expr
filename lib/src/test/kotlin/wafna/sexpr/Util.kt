package wafna.sexpr

import kotlin.test.assertEquals

fun assertAtom(expected: SExpr, actual: String) {
    val expected = expected as SAtom
    assertEquals(String(expected.data), actual)
}

