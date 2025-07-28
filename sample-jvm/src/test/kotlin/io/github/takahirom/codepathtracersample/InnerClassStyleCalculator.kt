package io.github.takahirom.codepathtracersample

// This class mimics inner class behavior but as an independent class
// to test if the issue is specifically with $ in class names or inner class mechanics
class InnerClassStyleCalculator {
    fun add(a: Int, b: Int): Int {
        println("  InnerClassStyleCalculator: Adding $a + $b")
        return a + b
    }
    
    fun complexCalculation(x: Int, y: Int): Int {
        println("  InnerClassStyleCalculator: Starting complex calculation")
        val sum = add(x, y)  // Nested call
        return sum * 2
    }
}

// Test class that simulates inner class naming
class TestClassInnerStyle {
    fun multiply(a: Int, b: Int): Int {
        println("  TestClassInnerStyle: Multiplying $a * $b")
        return a * b
    }
}