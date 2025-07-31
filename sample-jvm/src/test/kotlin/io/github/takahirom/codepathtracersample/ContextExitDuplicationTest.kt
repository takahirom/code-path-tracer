package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Test cases to verify the improved context exit tracking mechanism
 * and prevention of context enter/exit duplication
 */
class ContextExitDuplicationTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace methods B1 and B2 to test context deduplication
            event.className.contains("DuplicationTest") && 
            (event.methodName == "b1" || event.methodName == "b2")
        }
        .beforeContextSize(1)  // Show 1 level of context (method A)
        .asJUnitRule()
    
    @Test
    fun testContextExitTrackingWithDuplicationPrevention() {
        println("=== Testing context exit tracking and duplication prevention ===")
        
        val output = captureOutput {
            val test = DuplicationTestHierarchy()
            test.a()  // a -> b1, a -> b2
        }
        
        println("=== Test completed - A should appear only once for enter and once for exit ===")
        
        // Extract trace lines (lines with → or ←) - preserve indentation!
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        // Verify expected output structure: should have exactly 6 trace lines
        assertEquals("Should have exactly 6 trace lines", 6, traceLines.size)
        
        // Verify the exact sequence with proper indentation
        assertEquals("Should show context enter for a()", "→ DuplicationTestHierarchy.a()", traceLines[0])
        assertEquals("Should show method enter for b1()", "  → DuplicationTestHierarchy.b1()", traceLines[1])
        assertEquals("Should show method exit for b1", "  ← DuplicationTestHierarchy.b1", traceLines[2])
        assertEquals("Should show method enter for b2()", "  → DuplicationTestHierarchy.b2()", traceLines[3])
        assertEquals("Should show method exit for b2", "  ← DuplicationTestHierarchy.b2", traceLines[4])
        assertEquals("Should show context exit for a", "← DuplicationTestHierarchy.a", traceLines[5])
        
        // Critical: Verify no duplicates (with indentation preserved)
        val contextEnters = traceLines.count { it.contains("→ DuplicationTestHierarchy.a()") }
        assertEquals("Context enter for a should appear exactly once (no duplicates)", 1, contextEnters)
        
        val contextExits = traceLines.count { it.contains("← DuplicationTestHierarchy.a") }
        assertEquals("Context exit for a should appear exactly once (no duplicates)", 1, contextExits)
        
        // Additional duplication check with trimmed strings (ignore indentation)
        val trimmedLines = traceLines.map { it.trim() }
        val trimmedContextEnters = trimmedLines.count { it == "→ DuplicationTestHierarchy.a()" }
        val trimmedContextExits = trimmedLines.count { it == "← DuplicationTestHierarchy.a" }
        assertEquals("Context enter should appear exactly once (trimmed check)", 1, trimmedContextEnters)
        assertEquals("Context exit should appear exactly once (trimmed check)", 1, trimmedContextExits)
        
        // Verify that context enter appears before both b1 and b2
        val contextEnterIndex = traceLines.indexOfFirst { it.contains("→ DuplicationTestHierarchy.a()") }
        val b1EnterIndex = traceLines.indexOfFirst { it.contains("→ DuplicationTestHierarchy.b1()") }
        val b2EnterIndex = traceLines.indexOfFirst { it.contains("→ DuplicationTestHierarchy.b2()") }
        
        assertTrue("Context enter should appear before b1 enter", contextEnterIndex < b1EnterIndex)
        assertTrue("Context enter should appear before b2 enter", contextEnterIndex < b2EnterIndex)
        
        // Verify that context exit appears after both b1 and b2 exits
        val contextExitIndex = traceLines.indexOfFirst { it.contains("← DuplicationTestHierarchy.a") }
        val b1ExitIndex = traceLines.indexOfFirst { it.contains("← DuplicationTestHierarchy.b1") }
        val b2ExitIndex = traceLines.indexOfFirst { it.contains("← DuplicationTestHierarchy.b2") }
        
        assertTrue("Context exit should appear after b1 exit", contextExitIndex > b1ExitIndex)
        assertTrue("Context exit should appear after b2 exit", contextExitIndex > b2ExitIndex)
        
        // Verify proper indentation (hierarchical structure)
        assertTrue("Context enter should have no indentation", traceLines[0].startsWith("→"))
        assertTrue("Method b1 enter should be indented", traceLines[1].startsWith("  →"))
        assertTrue("Method b1 exit should be indented", traceLines[2].startsWith("  ←"))
        assertTrue("Method b2 enter should be indented", traceLines[3].startsWith("  →"))
        assertTrue("Method b2 exit should be indented", traceLines[4].startsWith("  ←"))
        assertTrue("Context exit should have no indentation", traceLines[5].startsWith("←"))
        
        // Verify consistent indentation depth
        val contextDepth = traceLines[0].takeWhile { it == ' ' }.length
        val methodDepth = traceLines[1].takeWhile { it == ' ' }.length
        assertEquals("Context methods should have depth 0", 0, contextDepth)
        assertEquals("Filtered methods should have depth 1 (2 spaces)", 2, methodDepth)
        
        // All filtered methods should have same indentation
        for (i in 1..4) {
            val actualDepth = traceLines[i].takeWhile { it == ' ' }.length
            assertEquals("All filtered method lines should have same indentation", methodDepth, actualDepth)
        }
    }
    
    @Test
    fun testNestedContextExitTracking() {
        println("=== Testing nested context exit tracking ===")
        
        val test = NestedTestHierarchy()
        test.outer()  // outer -> middle -> inner (only inner is filtered)
        
        println("=== Nested test completed ===")
        
        // Expected output with beforeContextSize=1:
        // -> middle   # context enter (generated when inner enters)
        //   -> inner  # actual filtered method  
        //   <- inner  # actual filtered method exit
        // <- middle   # context exit (generated when middle actually exits)
    }
    
    private fun captureOutput(action: () -> Unit): String {
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        
        try {
            action()
        } finally {
            System.setOut(originalOut)
        }
        
        return outputStream.toString()
    }
}

class DuplicationTestHierarchy {
    fun a() {
        println("Executing a")
        b1()
        b2()
    }
    
    fun b1() {
        println("Executing b1")
    }
    
    fun b2() {
        println("Executing b2") 
    }
}

class NestedTestHierarchy {
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
    }
}