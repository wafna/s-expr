package wafna.sexpr

import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows
import java.awt.Color

data class PrimitivesOnly(
    val name: String,
    val count: Int,
    val value: Double
)

data class Nullables(
    val name: String?,
    val count: Int?,
    val value: Double?
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

sealed interface Sealed {
    data class Sealed1(val sealed1: Int) : Sealed
    data class Sealed2(val sealed2: String) : Sealed
    data class Sealed3(val sealed3: Double) : Sealed
}

data class SealedContainer(val sealed: Sealed)

enum class EnumC {
    Bing, Bang, Boom
}

data class EnumContainerC(val enumC: EnumC)

enum class EnumP(val x: Int) {
    Bing(1), Bang(2), Boom(3)
}

data class EnumContainerP(val enumP: EnumP)

class TestSerdes {
    @Test
    fun primitives() {
        Serdes {
            register<PrimitivesOnly>()
        }.testObject(PrimitivesOnly("foo", 42, PI))
    }
    @Test
    fun listsOfPrimitives() {
        Serdes {
            register<ListsOfPrimitives>()
        }.testObject(listsOfPrimitives)
    }
    @Test
    fun listsOfListsOfPrimitives() {
        Serdes {
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
        Serdes {
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
    fun nullables() {
        Serdes { register<Nullables>() }
            .testObject(Nullables(null, null, null))
            .testObject(Nullables("foo", 42, PI))
    }
    @Test
    fun list() {
        Serdes().testObject(listOf(1, 2, 3))
        Serdes().testObject(List(3) { x -> List(3) { y -> x + y } })
        Serdes { register<PrimitivesOnly>() }
            .testObject(List(4) { PrimitivesOnly("$it", it, it.toDouble()) })
    }
    @Test
    fun set() {
        Serdes().testObject(setOf(1, 2, 3))
    }
    @Test
    fun pair() {
        Serdes().testObject(9 to 5)
        Serdes().testObject("positions" to listOf(1, 2, 3))
    }
    @Test
    fun map() {
        Serdes().testObject(mapOf(1 to 2, 3 to 4))
    }
    @Test
    fun arrayNotSupported() {
        assertThrows<Throwable> {
            Serdes().testObject(arrayOf(1, 2, 3, 4))
        }
    }
    @Test
    fun sealedDataClasses() {
        Serdes {
            register<Sealed>()
            register<SealedContainer>()
        }
            .testObject(Sealed.Sealed1(42))
            .testObject(Sealed.Sealed2("42"))
            .testObject(Sealed.Sealed3(42.0))
            .testObject(SealedContainer(Sealed.Sealed1(42)))
            .testObject(SealedContainer(Sealed.Sealed2("42")))
            .testObject(SealedContainer(Sealed.Sealed3(42.0)))
    }
    @Test
    fun enumC() {
        Serdes {
            register<EnumC>()
            register<EnumContainerC>()
        }.apply {
            EnumC.entries.forEach { testObject(it) }
            EnumC.entries.forEach { testObject(EnumContainerC(it)) }
        }
    }
    @Test
    fun enumP() {
        Serdes {
            register<EnumP>()
            register<EnumContainerP>()
        }.apply {
            EnumP.entries.forEach { testObject(it) }
            EnumP.entries.forEach { testObject(EnumContainerP(it)) }
        }
    }
    @Test
    fun custom() {
        val serdes = Serdes {
            serde(object : Serde<Color> {
                override fun toSExpr(obj: Color): SExpr = buildSExpr{
                    list { atom("red"); atom(obj.red.toString()) }
                    list { atom("green"); atom(obj.green.toString()) }
                    list { atom("blue"); atom(obj.blue.toString()) }
                }

                override fun fromSExpr(expr: SExpr): Color = buildMap {
                    expr.requireList().exprs.forEach { e ->
                        val (name, color) = e.requireList().exprs
                        put(name.requireAtom().asString(), color.requireAtom().asString().toInt())
                    }
                }.let {
                    Color(it.getValue("red"), it.getValue("green"), it.getValue("blue"))
                }
            })
        }
        val color = Color(10, 20, 30)
        serdes.toSExpr(color).let { s ->
            val u = serdes.fromSExpr<Color>(s)
            require(u.red == color.red)
            require(u.green == color.green)
            require(u.blue == color.blue)
        }
    }

    companion object {
        inline fun <reified T> Serdes.testObject(expected: T): Serdes = apply {
            val expr = toSExpr(expected)
            // println(expr.showSExpr())
            val actual = fromSExpr<T>(expr)
            assertEquals(expected, actual)
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