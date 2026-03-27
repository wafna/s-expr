package wafna.sexpr

import kotlin.test.Test
import kotlin.test.assertEquals

class TestByteStream {
    @Test
    fun test() {
        val stream = ByteStream.from("abc")
        assertEquals('a'.code.toByte(), stream.peek())
        assertEquals('a'.code.toByte(), stream.peek())
        assertEquals('a'.code.toByte(), stream.take())
        assertEquals('b'.code.toByte(), stream.peek())
        assertEquals('b'.code.toByte(), stream.peek())
        assertEquals('b'.code.toByte(), stream.take())
        assertEquals('c'.code.toByte(), stream.take())
        assertEquals(null, stream.peek())
        assertEquals(null, stream.take())
    }
}