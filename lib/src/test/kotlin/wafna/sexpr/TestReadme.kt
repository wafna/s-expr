package wafna.sexpr

import kotlin.test.Test

/**
 * Test the blocks of code in the README.
 */
class TestReadme {
    @Test
    fun readme() {
        data class Thing(val id: Int, val name: String)

        val mappers = Mappers {
            // Register adapters for data classes in this "constructor" block.
            register<Thing>()
        }
        val thing = Thing(42, "Banana")
        // All serialization goes through the one object.
        val expr = mappers.toSExpr(thing)
        // Note that converting strings to and from s-expressions and converting objects to and from s-expressions
        // are distinctly separate.
        println(expr.showSExpr())
        // Recreate the object from the s-expression.
        val actualFromExpr = mappers.fromSExpr<Thing>(expr)
        require(thing == actualFromExpr)
        // Recreate the object from the parsed, canonicalized s-expression.
        val actualFromString = mappers.fromSExpr<Thing>(readSExpr(CharStream.from(expr.showSExpr())))
        require(thing == actualFromString)
    }
}