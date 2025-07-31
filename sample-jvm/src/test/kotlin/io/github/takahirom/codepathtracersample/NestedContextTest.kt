package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

/**
 * Test cases specifically for nested context hierarchy tracking
 * Tests deeper nesting patterns that can't be tested in ContextExitDuplicationTest
 */
class NestedContextTest {
    
    @get:Rule
    val nestedTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace the innermost method to test nested context display
            event.className.contains("NestedTestHierarchy") && event.methodName == "inner"
        }
        .beforeContextSize(1)  // Show 1 level of context (middle method)
        .asJUnitRule()
    
    @Test
    fun testSingleLevelNestedContext() {
        println("=== Testing single level nested context (beforeContextSize=1) ===")
        
        val output = captureOutput {
            val test = NestedTestHierarchy()
            test.outer()  // outer -> middle -> inner (only inner filtered, middle shown as context)
        }
        
        println("=== Nested context test completed ===")
        
        val traceLines = output.lines().filter { it.contains("→") || it.contains("←") }
        
        // Verify we have the expected 4 trace lines
        assertEquals("Should have exactly 4 trace lines", 4, traceLines.size)
        
        // Verify exact sequence with proper indentation
        assertEquals("Should show context enter for middle", "→ NestedTestHierarchy.middle()", traceLines[0])
        assertEquals("Should show method enter for inner", "  → NestedTestHierarchy.inner()", traceLines[1])
        assertEquals("Should show method exit for inner", "  ← NestedTestHierarchy.inner", traceLines[2])
        assertEquals("Should show context exit for middle", "← NestedTestHierarchy.middle", traceLines[3])
        
        // Verify indentation levels
        assertTrue("Context enter should have no indentation", traceLines[0].startsWith("→"))
        assertTrue("Method enter should be indented", traceLines[1].startsWith("  →"))
        assertTrue("Method exit should be indented", traceLines[2].startsWith("  ←"))
        assertTrue("Context exit should have no indentation", traceLines[3].startsWith("←"))
        
        // Verify outer method is NOT shown (beyond beforeContextSize=1)
        assertFalse("Should not show outer method", traceLines.any { it.contains("outer") })
    }
}

/**
 * Test class for deeper nested context hierarchy
 */
class DeepNestedContextTest {
    
    @get:Rule
    val deepNestedRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace the deepest method
            event.className.contains("DeepNestHierarchy") && event.methodName == "deepest"
        }
        .beforeContextSize(2)  // Show 2 levels of context (middle and inner)
        .asJUnitRule()
    
    @Test
    fun testMultiLevelNestedContext() {
        println("=== Testing multi-level nested context (beforeContextSize=2) ===")
        
        val output = captureOutput {
            val test = DeepNestHierarchy()
            test.outer()  // outer -> middle -> inner -> deepest
        }
        
        println("=== Deep nested context test completed ===")
        
        val traceLines = output.lines().filter { it.contains("→") || it.contains("←") }
        
        // With beforeContextSize=2, we should see: middle, inner (context) + deepest (actual method)
        assertEquals("Should have exactly 6 trace lines", 6, traceLines.size)
        
        // Verify the sequence shows 2 levels of context
        assertEquals("Should show context enter for middle", "→ DeepNestHierarchy.middle()", traceLines[0])
        assertEquals("Should show context enter for inner", "  → DeepNestHierarchy.inner()", traceLines[1])
        assertEquals("Should show method enter for deepest", "    → DeepNestHierarchy.deepest()", traceLines[2])
        assertEquals("Should show method exit for deepest", "    ← DeepNestHierarchy.deepest", traceLines[3])
        assertEquals("Should show context exit for inner", "  ← DeepNestHierarchy.inner", traceLines[4])
        assertEquals("Should show context exit for middle", "← DeepNestHierarchy.middle", traceLines[5])
        
        // Verify correct indentation levels (0, 2, 4 spaces)
        assertEquals("Middle context should have 0 spaces", 0, traceLines[0].takeWhile { it == ' ' }.length)
        assertEquals("Inner context should have 2 spaces", 2, traceLines[1].takeWhile { it == ' ' }.length)
        assertEquals("Deepest method should have 4 spaces", 4, traceLines[2].takeWhile { it == ' ' }.length)
        
        // Verify outer method is NOT shown (beyond beforeContextSize=2)
        assertFalse("Should not show outer method", traceLines.any { it.contains("outer") })
    }
}

class DeepNestHierarchy {
    fun outer() {
        println("Executing outer")
        middle()
    }
    
    fun middle() {
        println("Executing middle")
        inner()
    }
    
    fun inner() {
        println("Executing inner")
        deepest()
    }
    
    fun deepest() {
        println("Executing deepest")
    }
}