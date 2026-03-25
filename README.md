# s-expr

S-expressions are the simplest possible data structure, 
consisting solely of lists of other s-expressions and atoms, 
which contain arrays of bytes.
Importantly, there is no restriction on the values of the bytes.
This makes the format extremely efficient for all forms of data, esp. binary.

This library is built for Kotlin and natively supports List, Set, Pair, Map, and Enum.
There is also built-in support for sealed, single level data class hierarchies. 

For literal s-expressions, the parser recognizes bare words (e.g. C style identifiers), 
double-quoted strings (e.g. C style strings), and run length encoded atoms. 
This implementation uses square brackets instead of parentheses for literal s-expressions 
to reduce strain on the shift key.

## Quick Start

Convert objects to s-expressions and back again.

```kotlin
// Domain objects.

enum class Position {
    Center, Guard, Forward
}

data class Player(val number: Int, val position: Position)

// Single level sealed data class hierarchy.
sealed interface Jersey {
    data class Home(val colors: List<Color>) : Jersey
    data class Away(val colors: List<Color>) : Jersey
}

// Lists and other (reified) collections are built in.
data class Team(
    val jerseys: List<Jersey>,
    val players: List<Player>,
)

// Register adapters in this "constructor" block.
val mappers = Mappers {
    // Register any enums to be used.
    register<Position>()
    register<Player>()
    // Only the top of the hierarchy is required.
    register<Jersey>()
    register<Team>()
    // Custom mapper for Color (which is not a data class).
    register(object : Mapper<Color> {
        // DSL for building s-expressions.
        override fun toSExpr(obj: Color): SExpr = buildSExpr {
            atom(obj.red.toString())
            atom(obj.green.toString())
            atom(obj.blue.toString())
        }

        override fun fromSExpr(expr: SExpr): Color = expr.requireList().exprs.let { list ->
            // DSL for reading s-expressions.
            fun field(index: Int) = list[index].requireAtom().asString().toInt()
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
```

## Features

* Support for List, Set, Pair, and Map collection types.
* Support for (single level) sealed data class hierarchies.
* Support for enums.
* Custom mappers.
