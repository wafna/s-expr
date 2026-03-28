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
```
<details>
<summary>Pretty Output</summary>
<pre>
[
  [name Damocles]
  [jerseys
    [
      [Home
        [
          [colors
            [
              ["255" "0" "0"]
              ["255" "255" "0"]]]]]
      [Away
        [
          [colors
            [
              ["0" "0" "255"]
              ["0" "0" "0"]]]]]]]
  [players
    [
      [
        [id "d54482ee-ad18-4728-bf42-a661ec03866f"]
        [number "42"]
        [position Goalie]]
      [
        [id "7d497dc7-2ea1-430a-acf2-ccbb8412d438"]
        [number "11"]
        [position Winger]]
      [
        [id "41fb047e-6fef-42d2-8fb1-d9474c5cb9ae"]
        [number "9"]
        [position Tender]]
      [
        [id "d0e5b8f0-f6b1-480b-a610-57cb9a9d8ad6"]
        [number "14"]
        [position Hacker]]
      [
        [id "2176fc44-fcf1-4d71-8b24-58edc936f081"]
        [number "2"]
        [position Forward]]]]]
</pre>
</details>

<details>
<summary>Canonical Output</summary>
<pre>[[4:name8:Damocles][7:jerseys[[4:Home[[6:colors[[3:2551:01:0][3:2553:2551:0]]]]][4:Away[[6:colors[[1:01:03:255][1:01:01:0]]]]]]][7:players[[[2:id36:d54482ee-ad18-4728-bf42-a661ec03866f][6:number2:42][8:position6:Goalie]][[2:id36:7d497dc7-2ea1-430a-acf2-ccbb8412d438][6:number2:11][8:position6:Winger]][[2:id36:41fb047e-6fef-42d2-8fb1-d9474c5cb9ae][6:number1:9][8:position6:Tender]][[2:id36:d0e5b8f0-f6b1-480b-a610-57cb9a9d8ad6][6:number2:14][8:position6:Hacker]][[2:id36:2176fc44-fcf1-4d71-8b24-58edc936f081][6:number1:2][8:position7:Forward]]]]]
</pre>
</details>

## Mapper Features

* Supports **data classes** and **enums**.
* Built-in support for multi-level sealed data class hierarchies.
* Built-in support for the *List*, *Set*, *Pair*, and *Map* collection types.
* Built-in support for the *Int*, *Long*, *Short*, *Double*, *Float*, *Char*, *String*, *Byte*, and *Boolean* atomic types
* Custom mappers.

For serializing primitives, the *Mapper* uses the JVM native literal representations.
This avoids endian impedance mismatches.
