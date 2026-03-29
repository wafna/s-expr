package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color

internal data class PrimitivesOnly(
    val boolean: Boolean = true,
    val byte: Byte = Byte.MIN_VALUE,
    val char: Char = '$',
    val string: String = "string",
    val short: Short = Short.MIN_VALUE,
    val int: Int = Int.MIN_VALUE,
    val long: Long = Long.MAX_VALUE,
    val double: Double = Double.MIN_VALUE,
    val float: Float = Float.MIN_VALUE,
)

internal data class Nullables(
    val name: String?,
    val count: Int?,
    val value: Double?
)

internal data class ListsOfPrimitives(
    val names: List<String>,
    val counts: List<Int>,
    val values: List<Double>
)

internal data class ListOfListOfPrimitives(
    val groups: List<List<String>>
)

internal data class NestedObjects(
    val primitivesOnly: PrimitivesOnly,
    val listsOfPrimitives: ListsOfPrimitives
)

enum class EnumC {
    Bing, Bang, Boom
}

data class EnumContainerC(val enumC: EnumC)

enum class EnumP(val x: Int) {
    Bing(1), Bang(2), Boom(3)
}

data class EnumContainerP(val enumP: EnumP)

class TestMappers {
    @Test
    fun primitives() {
        Mappers {
            adapt<PrimitivesOnly>()
        }.testObject(PrimitivesOnly())
    }
    @Test
    fun listsOfPrimitives() {
        Mappers {
            adapt<ListsOfPrimitives>()
        }.testObject(listsOfPrimitives)
    }
    @Test
    fun listsOfListsOfPrimitives() {
        Mappers {
            adapt<ListOfListOfPrimitives>()
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
        Mappers {
            adapt<PrimitivesOnly>()
            adapt<ListsOfPrimitives>()
            adapt<NestedObjects>()
        }.testObject(NestedObjects(PrimitivesOnly(), listsOfPrimitives))
    }
    @Test
    fun nullables() {
        Mappers { adapt<Nullables>() }.run {
            testObject(Nullables(null, null, null))
            testObject(Nullables("foo", 42, PI))
        }
    }
    @Test
    fun list() {
        Mappers().testObject(listOf(1, 2, 3))
        Mappers().testObject(List(3) { x -> List(3) { y -> x + y } })
        Mappers { adapt<PrimitivesOnly>() }
            .testObject(List(4) { PrimitivesOnly() })
    }
    @Test
    fun set() {
        Mappers().testObject(setOf(1, 2, 3))
    }
    @Test
    fun pair() {
        Mappers().testObject(9 to 5)
        Mappers().testObject("positions" to listOf(1, 2, 3))
    }
    @Test
    fun map() {
        Mappers().testObject(mapOf(1 to 2, 3 to 4))
    }
    @Test
    fun arrayNotSupported() {
        assertThrows<Throwable> {
            Mappers().testObject(arrayOf(1, 2, 3, 4))
        }
    }
    @Test
    fun enumC() {
        Mappers {
            adapt<EnumC>()
            adapt<EnumContainerC>()
        }.apply {
            EnumC.entries.forEach { testObject(it) }
            EnumC.entries.forEach { testObject(EnumContainerC(it)) }
        }
    }
    @Test
    fun enumP() {
        Mappers {
            adapt<EnumP>()
            adapt<EnumContainerP>()
        }.apply {
            EnumP.entries.forEach { testObject(it) }
            EnumP.entries.forEach { testObject(EnumContainerP(it)) }
        }
    }
    @Test
    fun custom() {
        val mappers = Mappers {
            register(object : Mapper<Color> {
                override fun toSExpr(obj: Color): SExpr = buildSExpr {
                    atom(obj.red.toString())
                    atom(obj.green.toString())
                    atom(obj.blue.toString())
                }

                override fun fromSExpr(expr: SExpr): Color = expr.requireList().let { list ->
                    fun field(index: Int) = list.exprs[index].requireAtom().asString()!!.toInt()
                    Color(field(0), field(1), field(2))

                }
            })
        }
        val color = Color(10, 20, 30)
        mappers.fromSExpr<Color>(mappers.toSExpr(color)).apply {
            require(red == color.red)
            require(green == color.green)
            require(blue == color.blue)
        }
    }

    companion object {
        internal val listsOfPrimitives = ListsOfPrimitives(
            names = listOf("foo", "bar"),
            counts = listOf(1, 2, 3),
            values = listOf(PI, E)
        )
    }
}