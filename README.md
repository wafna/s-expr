# s-expr

S-expressions are the simplest possible data structure, 
consisting solely of lists of other s-expressions and atoms, 
which contain arrays of bytes.
Importantly, there is no restriction on the values of the bytes.
This makes the format extremely efficient for all forms of data, esp. binary.

For literal s-expressions, the parser recognizes bare words ()

This implementation uses square brackets instead of parentheses to reduce strain on the shift key.

This package includes:

* Parser.

Parses streams of bytes into s-expressions.

* Builder.

Create literal s-expressions in the manner of Kotlin's `buildMap`.

* Writer.

Turn s-expressions into streams of bytes.

* Adapters.

Serialize data classes using s-expressions.