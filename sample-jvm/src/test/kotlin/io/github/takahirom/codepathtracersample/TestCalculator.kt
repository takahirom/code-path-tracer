package io.github.takahirom.codepathtracersample

class TestCalculator {
    fun add(a: Int, b: Int): Int {
        println("  Calculator: Adding $a + $b")
        return a + b
    }
    
    fun multiply(a: Int, b: Int): Int {
        println("  Calculator: Multiplying $a * $b")
        return a * b
    }
    
    fun complexCalculation(x: Int, y: Int): Int {
        println("  Calculator: Starting complex calculation with $x and $y")
        val sum = add(x, y)
        val doubled = multiply(sum, 2)
        val final = add(doubled, 12)
        println("  Calculator: Complex calculation complete")
        return final
    }
}