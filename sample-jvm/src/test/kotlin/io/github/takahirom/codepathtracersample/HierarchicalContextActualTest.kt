package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class HierarchicalContextActualTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace add method to test hierarchical context
            event.className.contains("HierarchicalCalculator") && event.methodName == "add"
        }
        .beforeContextSize(3)  // Show up to 3 levels of context hierarchy
        .asJUnitRule()
    
    @Test
    fun testHierarchicalContextWithRealTracing() {
        println("=== Testing hierarchical context with actual tracing ===")
        
        val calculator = HierarchicalCalculator()
        // This should create a call chain: testHierarchicalContextWithRealTracing -> complexCalculation -> add
        val result = calculator.complexCalculation(5, 3)
        
        assertEquals("Result should be correct", 24, result)
        println("=== Context test completed - check for [context] output above ===")
    }
}

class HierarchicalCalculator {
    fun add(a: Int, b: Int): Int {
        println("Calculator: Adding $a + $b")
        return a + b
    }
    
    fun multiply(a: Int, b: Int): Int {
        println("Calculator: Multiplying $a * $b")
        return a * b
    }
    
    fun complexCalculation(x: Int, y: Int): Int {
        println("Calculator: Starting complex calculation with $x and $y")
        val sum = add(x, y)  // This should show context
        val doubled = multiply(sum, 2)
        val final = add(doubled, x + y)  // This should also show context
        println("Calculator: Complex calculation complete")
        return final
    }
}