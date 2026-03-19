package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals

class TestCharStream {
    @Test
    fun test() {
        val stream = CharStream("abc".iterator())
        assertEquals('a', stream.peek())
        assertEquals('a', stream.peek())
        assertEquals('a', stream.take())
        assertEquals('b', stream.peek())
        assertEquals('b', stream.peek())
        assertEquals('b', stream.take())
        assertEquals('c', stream.take())
        assertEquals(null, stream.peek())
        assertEquals(null, stream.take())
    }
}