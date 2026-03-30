package wafna.sexpr

/*
Sample code from the README
*/

import java.awt.Color
import java.util.*

// Domain objects.

enum class Position {
    Goalie, Forward, Hacker, Tender, Winger
}

data class Player(val id: UUID, val number: Int, val position: Position)

sealed interface Jersey {
    data class Home(val colors: List<Color>) : Jersey
    data class Away(val colors: List<Color>) : Jersey
}

data class Team(
    val name: String,
    val jerseys: List<Jersey>,
    val players: List<Player>,
)

// Custom mapper for Color (which is not a data class).
val colorMapper = object : Mapper<Color> {
    // Use the builder DSL to construct a list of the RGB values.
    override fun toSExpr(obj: Color, listener: Listener) {
        with(listener) {
            listener.list {
                atom(SBytes(obj.red.toString().bytes()))
                atom(SBytes(obj.green.toString().bytes()))
                atom(SBytes(obj.blue.toString().bytes()))
            }
        }
    }

    // Use the reader DSL to narrow types and access the data in lists and atoms.
    override fun fromSExpr(expr: SExpr): Color = expr.requireList().exprs.let { list ->
        fun field(index: Int) = list[index].requireBytes().asString()!!.toInt()
        Color(field(0), field(1), field(2))
    }
}

// Custom mapper for UUID, which only needs an atom.
val uuidMapper = object : Mapper<UUID> {
    override fun toSExpr(obj: UUID, listener: Listener) {
        listener.atom(SBytes(obj.toString().bytes()))
    }

    override fun fromSExpr(expr: SExpr): UUID = UUID.fromString(expr.requireBytes().data.string())
}

// Register adapters in this "constructor" block.
// Note: register dependent types before containing types.
val mappers = Mappers {
    adapt<Position>()
    register(colorMapper)
    register(uuidMapper)
    adapt<Player>()
    adapt<Jersey>()
    adapt<Team>()
}

fun main() {
    val team = Team(
        name = "Damocles",
        jerseys = listOf(
            Jersey.Home(listOf(Color.RED, Color.YELLOW)),
            Jersey.Away(listOf(Color.BLUE, Color.BLACK))
        ),
        players = listOf(
            Player(UUID.randomUUID(), 42, Position.Goalie),
            Player(UUID.randomUUID(), 11, Position.Winger),
            Player(UUID.randomUUID(), 9, Position.Tender),
            Player(UUID.randomUUID(), 14, Position.Hacker),
            Player(UUID.randomUUID(), 2, Position.Forward),
        )
    )

    // All conversion goes through the mappers object.
    // S-expressions can also be serialized directly to output streams.
    val expr = mappers.toSExpr<Team>(team)
    // Note that converting strings to and from s-expressions
    // and converting objects to and from s-expressions are distinct functions.
    // In this example we modify the default settings (canonical) to make the output readable.
    // In this mode, the writer will prefer, in order, bare atoms, then literal strings,
    // then run length encoded atoms.
    val pretty = expr.showSExpr {
        dataFormat = DataFormat.Readable
        indent = 2
    }
    println("Pretty: ${pretty.length} bytes.")
    println(pretty)
    // Recreate the object from the s-expression.
    val actualFromExpr = mappers.fromSExpr<Team>(expr)
    require(team == actualFromExpr)
    // The canonical representation uses run length encoded atoms exclusively with no added whitespace.
    val canonical = expr.showSExpr()
    println("Canonical: ${canonical.length} bytes.")
    println(canonical)
    // Recreate the object from the canonicalized s-expression.
    val actualFromBytes = mappers.fromSExpr<Team>(readSExpr(ByteStream.from(canonical)))
    require(team == actualFromBytes)
}