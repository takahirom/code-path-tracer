package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

class BeforeContextHierarchyTest {
    
    @get:Rule
    val testRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace method C to test context display
            event.className.contains("TestHierarchy") && event.methodName == "c"
        }
        .beforeContextSize(1)  // Show 1 level of context
        .asJUnitRule()

    @Test
    fun testBeforeContextSize1ShowsHierarchicalStructure() {
        println("=== Starting test ===")
        
        val output = captureOutput {
            val test = TestHierarchy()
            test.a()  // a -> b -> c, only c should pass filter but b should be shown as context
        }
        
        println("=== Test completed ===")
        
        // Extract trace lines (lines with → or ←) - preserve indentation!
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        // Verify expected output structure
        assertEquals("Should have exactly 4 trace lines", 4, traceLines.size)
        
        // Verify the exact sequence with proper indentation
        assertEquals("Should show context enter for b()", "→ TestHierarchy.b()", traceLines[0])
        assertEquals("Should show method enter for c()", "  → TestHierarchy.c()", traceLines[1])
        assertEquals("Should show method exit for c", "  ← TestHierarchy.c", traceLines[2])
        assertEquals("Should show context exit for b", "← TestHierarchy.b", traceLines[3])
        
        // Verify no duplicates
        val contextEnters = traceLines.count { it.contains("→ TestHierarchy.b()") }
        assertEquals("Context enter for b should appear exactly once", 1, contextEnters)
        
        val contextExits = traceLines.count { it.contains("← TestHierarchy.b") }
        assertEquals("Context exit for b should appear exactly once", 1, contextExits)
        
        // Verify proper indentation (hierarchical structure)
        assertTrue("Context enter should have no indentation", traceLines[0].startsWith("→"))
        assertTrue("Method enter should be indented with 2 spaces", traceLines[1].startsWith("  →"))
        assertTrue("Method exit should be indented with 2 spaces", traceLines[2].startsWith("  ←"))
        assertTrue("Context exit should have no indentation", traceLines[3].startsWith("←"))
        
        // Verify indentation depth consistency
        val contextDepth = traceLines[0].takeWhile { it == ' ' }.length
        val methodDepth = traceLines[1].takeWhile { it == ' ' }.length
        assertEquals("Context methods should have depth 0 (no indentation)", 0, contextDepth)
        assertEquals("Filtered methods should have depth 1 (2 spaces indentation)", 2, methodDepth)
        assertEquals("Method exit should have same depth as method enter", methodDepth, traceLines[2].takeWhile { it == ' ' }.length)
    }
    
    // TODO: Add test for beforeContextSize=2 later
    
}

class TestHierarchy {
    fun a() {
        println("Executing a")
        b()
    }
    
    fun b() {
        println("Executing b") 
        c()
    }
    
    fun c() {
        println("Executing c")
    }
}