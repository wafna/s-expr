package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

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

class TestAdapter {
    @Test
    fun primitives() {
        val adapters = Adapters().apply {
            register<PrimitivesOnly>()
        }
        val obj = PrimitivesOnly("foo", 42, PI)
        val expr = adapters.toSExpr(obj)
        assertEquals(obj, adapters.fromSExpr<PrimitivesOnly>(expr))
    }
    @Test
    fun listsOfPrimitives() {
        val adapters = Adapters().apply {
            register<ListsOfPrimitives>()
        }
        val obj = ListsOfPrimitives(
            listOf("foo", "bar"),
            listOf(1, 2, 3),
            listOf(PI, E)
        )
        val expr = adapters.toSExpr<ListsOfPrimitives>(obj)
        assertEquals(obj, adapters.fromSExpr<ListsOfPrimitives>(expr))
    }
    @Test
    fun listsOfListsOfPrimitives() {
        val adapters = Adapters().apply {
            register<ListOfListOfPrimitives>()
        }
        val obj = ListOfListOfPrimitives(
            listOf(
                listOf("bing", "bang", "boom"),
                listOf("herp", "derp"),
            )
        )
        val expr = adapters.toSExpr<ListOfListOfPrimitives>(obj)
        println(expr.writeToString())
        assertEquals(obj, adapters.fromSExpr<ListOfListOfPrimitives>(expr))
    }
    @Test
    fun bareList() {
        val adapters = Adapters()
        val list = listOf(1, 2, 3)
        val expr = adapters.toSExpr(list)
        assertEquals(list, adapters.fromSExpr<List<Int>>(expr))
    }
    @Test
    fun nestedObjects() {
        val adapters = Adapters().apply {
            register<PrimitivesOnly>()
            register<ListsOfPrimitives>()
            register<NestedObjects>()
        }
        val obj = NestedObjects(
            PrimitivesOnly("foo", 42, PI),
            ListsOfPrimitives(
                listOf("foo", "bar"),
                listOf(1, 2, 3),
                listOf(PI, E)
            )
        )
        val expr = adapters.toSExpr<NestedObjects>(obj)
        println(expr.writeToString())
        assertEquals(obj, adapters.fromSExpr<NestedObjects>(expr))
    }
}