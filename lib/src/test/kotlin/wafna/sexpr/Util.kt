package wafna.sexpr

import kotlin.test.assertEquals
import kotlin.test.fail
import java.io.ByteArrayOutputStream

fun parse(input: String) = readSExpr(ByteStream.from(input))

fun assertAtom(expected: SExpr, actual: String) {
    assertEquals(expected.requireBytes().data.string(), actual)
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

inline fun <reified T> Mappers.testObject(expected: T) {
    val expr = toSExpr(expected)
    val canonical = expr.showSExpr()
    val bytes = ByteArrayOutputStream().also {
        toSExpr(expected, StreamSink(it))
    }.toByteArray()
    assertEquals(canonical, bytes.string())
    //println(canonical)
    val actual = fromSExpr<T>(expr)
    assertEquals(expected, actual)
}

