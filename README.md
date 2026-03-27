# s-expr

S-expressions are the simplest possible data structure, 
consisting solely of lists of other s-expressions and atoms, 
which contain arrays of bytes.
Importantly, there is no restriction on the values of the bytes.
This makes the format extremely efficient for all forms of data, esp. binary.

This library provides facilities for converting s-expressions to and from strings and
converting data classes and enums to and from s-expressions.
This library is built for Kotlin and inherently supports the Java primitives, strings, and 
the List, Set, Pair, and Map collection types.
There is also built-in support for multiple levels of sealed data class hierarchies. 
This implementation also supports an explicit NULL atom, different from an empty atom.

For literal s-expressions, the parser recognizes bare words (C style identifiers), 
double-quoted strings (C style strings), and run length encoded atoms. 
This implementation uses square brackets instead of parentheses for literal s-expressions 
to reduce strain on the shift key.

## Quick Start

Convert objects to s-expressions and back again using the ***mapping*** facility.

```kotlin
import java.awt.Color
import java.util.*

// Domain objects.

enum class Position {
    Center, Guard, Forward
}

data class Player(val id: UUID, val number: Int, val position: Position)

sealed interface Jersey {
    data class Home(val colors: List<Color>) : Jersey
    data class Away(val colors: List<Color>) : Jersey
}

data class Team(
    val jerseys: List<Jersey>,
    val players: List<Player>,
)

// Custom mapper for Color (which is not a data class).
val colorMapper = object : Mapper<Color> {
    // Use the builder DSL to construct a list of the RGB values.
    override fun toSExpr(obj: Color): SExpr = buildSExpr {
        atom(obj.red.toString())
        atom(obj.green.toString())
        atom(obj.blue.toString())
    }

    // Use the reader DSL to narrow types and access the data in lists and atoms.
    override fun fromSExpr(expr: SExpr): Color = expr.requireList().exprs.let { list ->
        fun field(index: Int) = list[index].requireBytes().asString()!!.toInt()
        Color(field(0), field(1), field(2))
    }
}

// Custom mapper for UUID, which only needs an atom.
val uuidMapper = object : Mapper<UUID> {
    override fun toSExpr(obj: UUID): SExpr = SBytes(obj.toString().bytes())
    override fun fromSExpr(expr: SExpr): UUID = UUID.fromString(expr.requireBytes().data.string())
}

// Register adapters in this "constructor" block.
// Note: register dependent types before containing types.
val mappers = Mappers {
    register<Position>()
    register(colorMapper)
    register(uuidMapper)
    register<Player>()
    register<Jersey>()
    register<Team>()
}

fun main() {
    val team = Team(
        jerseys = listOf(
            Jersey.Home(listOf(Color.RED, Color.YELLOW)),
            Jersey.Away(listOf(Color.BLUE, Color.BLACK))
        ),
        players = listOf(
            Player(UUID.randomUUID(), 42, Position.Center),
            Player(UUID.randomUUID(), 11, Position.Guard),
            Player(UUID.randomUUID(), 9, Position.Guard),
            Player(UUID.randomUUID(), 14, Position.Forward),
            Player(UUID.randomUUID(), 2, Position.Forward),
        )
    )

    // All conversion goes through the mappers object.
    val expr = mappers.toSExpr<Team>(team)
    // Note that converting strings to and from s-expressions
    // and converting objects to and from s-expressions are separate.
    println(expr.showSExpr())
    // Recreate the object from the s-expression.
    val actualFromExpr = mappers.fromSExpr<Team>(expr)
    require(team == actualFromExpr)
    // Recreate the object from the canonicalized s-expression.
    val actualFromBytes = mappers.fromSExpr<Team>(readSExpr(ByteStream.from(expr.showSExpr())))
    require(team == actualFromBytes)
}
```

## Mapper Features

* Supports **data classes** and **enums**.
* Built-in support for multi-level sealed data class hierarchies.
* Built-in support for the *List*, *Set*, *Pair*, and *Map* collection types.
* Built-in support for the *Int*, *Long*, *Short*, *Double*, *Float*, *Char*, *String*, *Byte*, and *Boolean* atomic types
* Custom mappers.

For serializing primitives, the *Mapper* uses the JVM native literal representations.
This avoids endian impedance mismatches.
