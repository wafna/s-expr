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

* Builder.

Create literal s-expressions in the manner of Kotlin's builder functions.

* Writer.

Turn s-expressions into streams of bytes.