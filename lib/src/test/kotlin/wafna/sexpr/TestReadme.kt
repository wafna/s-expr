package wafna.sexpr

import kotlin.test.Test
import java.awt.Color

// Domain objects.

enum class Position {
    Center, Guard, Forward
}

data class Player(val number: Int, val position: Position)

sealed interface Jersey {
    data class Home(val colors: List<Color>) : Jersey
    data class Away(val colors: List<Color>) : Jersey
}

data class Team(
    val jerseys: List<Jersey>,
    val players: List<Player>,
)
/**
 * Test the blocks of code in the README.
 */
class TestReadme {
    @Test
    fun readme() {
        // Register adapters in this "constructor" block.
        val mappers = Mappers {
            register<Position>()
            register<Player>()
            register<Jersey>()
            register<Team>()
            // Custom mapper for Color (which is not a data class).
            register(object : Mapper<Color> {
                override fun toSExpr(obj: Color): SExpr = buildSExpr {
                    atom(obj.red.toString())
                    atom(obj.green.toString())
                    atom(obj.blue.toString())
                }

                override fun fromSExpr(expr: SExpr): Color = expr.requireList().exprs.let { list ->
                    fun field(index: Int) = list[index].mapAtom { asString().toInt() }!!
                    Color(field(0), field(1), field(2))
                }
            })
        }

        val team = Team(
            jerseys = listOf(
                Jersey.Home(listOf(Color.RED, Color.YELLOW)),
                Jersey.Away(listOf(Color.BLUE, Color.BLACK))
            ),
            players = listOf(
                Player(42, Position.Center),
                Player(11, Position.Guard),
                Player(9, Position.Guard),
                Player(14, Position.Forward),
                Player(2, Position.Forward),
            )
        )

        // All serialization goes through the one object.
        val expr = mappers.toSExpr<Team>(team)
        // Note that converting strings to and from s-expressions and converting objects to and from s-expressions
        // are distinctly separate.
        println(expr.showSExpr())
        // Recreate the object from the s-expression.
        val actualFromExpr = mappers.fromSExpr<Team>(expr)
        require(team == actualFromExpr)
        // Recreate the object from the parsed, canonicalized s-expression.
        val actualFromString = mappers.fromSExpr<Team>(readSExpr(CharStream.from(expr.showSExpr())))
        require(team == actualFromString)
    }
}