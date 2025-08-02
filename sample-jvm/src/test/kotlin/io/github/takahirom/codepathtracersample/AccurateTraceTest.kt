package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

/**
 * Test to reproduce and verify the accuracy of trace output
 */
class AccurateTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            event.className.contains("Calculator")
        }
        .asJUnitRule()
    
    @Test
    fun testComplexCalculationTracing() {
        val output = captureOutput {
            val calculator = Calculator()
            calculator.complexCalculation(5, 3)
        }
        
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        println("=== Complex calculation trace output ===")
        traceLines.forEach { println(it) }
        
        // Verify the exact trace sequence
        assertTrue("Should have trace output", traceLines.isNotEmpty())
        
        // Check that we see constructor
        val constructorEnters = traceLines.count { it.contains("→") && it.contains("<init>") }
        val constructorExits = traceLines.count { it.contains("←") && it.contains("<init>") }
        assertTrue("Should have constructor enter", constructorEnters > 0)
        assertTrue("Should have constructor exit", constructorExits > 0)
        
        // Check complexCalculation method
        val complexCalcEnters = traceLines.count { it.contains("→") && it.contains("complexCalculation") }
        val complexCalcExits = traceLines.count { it.contains("←") && it.contains("complexCalculation") }
        assertEquals("Should have one complexCalculation enter", 1, complexCalcEnters)
        assertEquals("Should have one complexCalculation exit", 1, complexCalcExits)
        
        // Check nested add/multiply calls
        val addEnters = traceLines.count { it.contains("→") && it.contains("add") }
        val multiplyEnters = traceLines.count { it.contains("→") && it.contains("multiply") }
        
        // Should see 2 add calls and 1 multiply call
        assertEquals("Should have 2 add method calls", 2, addEnters)
        assertEquals("Should have 1 multiply method call", 1, multiplyEnters)
    }
}

class Calculator {
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
    
    fun complexCalculation(x: Int, y: Int): Int {
        val sum = add(x, y)           // add(5, 3) = 8
        val doubled = multiply(sum, 2) // multiply(8, 2) = 16  
        val final = add(doubled, 12)   // add(16, 12) = 28
        return final
    }
}