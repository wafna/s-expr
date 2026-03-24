package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows

data class PrimitivesOnly(
    val name: String,
    val count: Int,
    val value: Double
)

data class ListsOfPrimitives(
    val names: List<String>,
    val counts: List<Int>,
    val values: List<Double>
)

data class ListOfListOfPrimitives(
    val groups: List<List<String>>
)

data class NestedObjects(
    val primitivesOnly: PrimitivesOnly,
    val listsOfPrimitives: ListsOfPrimitives
)

class TestSObjects {
    @Test
    fun primitives() {
        SObjects().apply {
            register<PrimitivesOnly>()
        }.testObject(PrimitivesOnly("foo", 42, PI))
    }
    @Test
    fun listsOfPrimitives() {
        SObjects().apply {
            register<ListsOfPrimitives>()
        }.testObject(listsOfPrimitives)
    }
    @Test
    fun listsOfListsOfPrimitives() {
        SObjects().apply {
            register<ListOfListOfPrimitives>()
        }.testObject(
            ListOfListOfPrimitives(
                listOf(
                    listOf("bing", "bang", "boom"),
                    listOf("herp", "derp"),
                )
            )
        )
    }
    @Test
    fun nestedObjects() {
        SObjects().apply {
            register<PrimitivesOnly>()
            register<ListsOfPrimitives>()
            register<NestedObjects>()
        }.testObject(
            NestedObjects(
                primitivesOnly,
                listsOfPrimitives
            )
        )
    }
    @Test
    fun list() {
        SObjects().testObject(listOf(1, 2, 3))
        SObjects().apply { register<PrimitivesOnly>() }
            .testObject(List(4) { PrimitivesOnly("foo", 42, PI) })
    }
    @Test
    fun set() {
        SObjects().testObject(setOf(1, 2, 3))
    }
    @Test
    fun pair() {
        SObjects().testObject(9 to 5)
        SObjects().testObject("positions" to listOf(1, 2, 3))
    }
    @Test
    fun map() {
        SObjects().testObject(mapOf(1 to 2, 3 to 4))
    }
    @Test
    fun arrayNotSupported() {
        assertThrows<Throwable> {
            SObjects().testObject(arrayOf(1, 2, 3, 4))
        }
    }

    companion object {
        inline fun <reified T> SObjects.testObject(obj: T) {
            val expr = toSExpr(obj)
            assertEquals(obj, fromSExpr(expr))
        }

        val primitivesOnly = PrimitivesOnly(
            name = "foo",
            count = 42,
            value = PI
        )
        val listsOfPrimitives = ListsOfPrimitives(
            names = listOf("foo", "bar"),
            counts = listOf(1, 2, 3),
            values = listOf(PI, E)
        )
    }
}