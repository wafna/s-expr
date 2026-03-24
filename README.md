# s-expr

S-expressions are the simplest possible data structure, 
consisting solely of lists of other s-expressions and atoms, 
which contain arrays of bytes.
Importantly, there is no restriction on the values of the bytes.
This makes the format extremely efficient for all forms of data, esp. binary.

For literal s-expressions, the parser recognizes bare words (e.g. C style identifiers), 
double quoted strings (e.g. C style strings), and run length encoded atoms. 

This implementation uses square brackets instead of parentheses for literal s-expressions 
to reduce strain on the shift key.

This package includes:


* Reader and Writer.

Turn s-expressions into streams of bytes and back again.
Features both "SAX" and "DOM" styles of parsing.

* Object mapping.

Serialize data classes and collections using s-expressions.

```kotlin
data class Thing(val id: Int, val name: String)

val serdes = Serdes {
    // Register adapters for data classes in this "constructor" block.
    register<Thing>()
}
val thing = Thing(42, "Banana")
// All serialization goes through the one object.
val expr = serdes.toSExpr(thing)
println(expr.showSExpr())
// Note that converting strings to and from s-expressions and converting objects to and from s-expressions
// are distinctly separate.
val actual = serdes.fromSExpr<Thing>(expr)
require(thing == actual)
```
Produces,
```text
[[2:id2:42][4:name6:Banana]]
```
* Builder.

Create literal s-expressions in the manner of Kotlin's builder functions.

```kotlin
buildSExpr {
    list {
        atom("id")
        atom("42")
    }
    list {
        atom("name")
        atom("Banana")
    }
}
```
