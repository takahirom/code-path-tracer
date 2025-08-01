package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

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
        val output = captureOutput {
            val calculator = HierarchicalCalculator()
            // This should create a call chain: testHierarchicalContextWithRealTracing -> complexCalculation -> add
            val result = calculator.complexCalculation(5, 3)
            assertEquals("Result should be correct", 24, result)
        }
        
        // Verify trace output contains expected context hierarchy
        val traceLines = output.lines().filter { it.contains("→") || it.contains("←") }
        assertTrue("Should contain hierarchical context traces", traceLines.isNotEmpty())
        
        // Verify we have proper context hierarchy showing
        val contextEnters = traceLines.filter { it.contains("→") && !it.trim().startsWith("      ") }
        val addMethodEnters = traceLines.filter { it.contains("→") && it.contains("add") }
        
        assertTrue("Should have context enters", contextEnters.isNotEmpty())
        assertTrue("Should have add method enters", addMethodEnters.isNotEmpty())
        
        // Verify hierarchical structure shows multiple levels
        val hasMultipleLevels = traceLines.any { it.startsWith("  ") } && traceLines.any { it.startsWith("   ") }
        assertTrue("Should show multiple indentation levels", hasMultipleLevels)
        
        // Verify specific methods are traced
        assertTrue("Should trace complexCalculation", traceLines.any { it.contains("complexCalculation") })
        assertTrue("Should trace add method", traceLines.any { it.contains("add") })
    }
}

class HierarchicalCalculator {
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
    
    fun complexCalculation(x: Int, y: Int): Int {
        val sum = add(x, y)  // This should show context
        val doubled = multiply(sum, 2)
        val final = add(doubled, x + y)  // This should also show context
        return final
    }
}