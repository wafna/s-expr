package wafna.sexpr

import kotlin.test.assertEquals

fun String.toCharStream() = CharStream(iterator())

fun parse(input: String) = parse(input.toCharStream())

fun assertAtom(expected: SExpr, actual: String) {
    val expected = expected as SAtom
    assertEquals(String(expected.data), actual)
}

