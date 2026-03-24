# s-expr

S-expressions are the simplest possible data structure, 
consisting solely of lists of other s-expressions and atoms, 
which contain arrays of bytes.
Importantly, there is no restriction on the values of the bytes.
This makes the format extremely efficient for all forms of data, esp. binary.

For literal s-expressions, the parser recognizes bare words (e.g. C style identifiers), 
double quoted strings (e.g. C style strings), and run length encoded atoms. 

This implementation uses square brackets instead of parentheses to reduce strain on the shift key.

This package includes:

* Object mapping.

Serialize data classes and collections using s-expressions with SObjects.

```kotlin
data class Thing(val id: Int, val name: String)
SObjects {
    register<Thing>()
}.run {
    val expr = toSExpr(Thing(42, "Banana"))
    println(expr.writeToString())
}
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

* Writer.

Turn s-expressions into streams of bytes.