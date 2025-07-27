package io.github.takahirom.codepathfinder

import org.junit.Test

class SimpleMethodTraceTest {
    
    @Test
    fun testBasicFunctionality() {
        println("Testing basic functionality without agent...")
        
        val calculator = SampleCalculator()
        val result = calculator.add(5, 3)
        println("Result: $result")
        
        // Basic assertion without JUnit assert methods
        if (result != 8) {
            throw AssertionError("Expected 8, got $result")
        }
        
        println("Test passed!")
    }
    
    class SampleCalculator {
        fun add(a: Int, b: Int): Int {
            println("Adding $a + $b")
            return a + b
        }
    }
}