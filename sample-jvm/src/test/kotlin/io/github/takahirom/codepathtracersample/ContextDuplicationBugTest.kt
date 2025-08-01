package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

/**
 * Test to reproduce the context duplication bug where the same method appears twice:
 * once as a normal filter match and once as a context method.
 * 
 * Scenario: A(B(C())) with beforeContextSize=1 and filter=method.contains("B|C")
 * Expected bug: B appears twice - once as filter match, once as context for C
 */
class ContextDuplicationBugTest {
    
    @get:Rule
    val contextDuplicationRule = CodePathTracer.Builder()
        .filter { event ->
            // Filter matches both B and C methods
            event.className.contains("DuplicationBugHierarchy") && 
            (event.methodName.contains("B") || event.methodName.contains("C"))
        }
        .beforeContextSize(1)  // Show 1 level of context
        .asJUnitRule()
    
    @Test
    fun testContextDuplicationBug() {
        val output = captureOutput {
            val test = DuplicationBugHierarchy()
            test.methodA()  // A -> B -> C, filter matches B and C
        }
        
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        println("=== Trace output ===")
        traceLines.forEach { println(it) }
        
        // Current bug: B appears twice
        // 1. As context for C (because beforeContextSize=1)
        // 2. As a filtered method itself (because it matches the filter)
        
        // Count occurrences of methodB in enter events
        val methodBEnterCount = traceLines.count { 
            it.contains("→") && it.contains("methodB") 
        }
        
        // Count occurrences of methodB in exit events  
        val methodBExitCount = traceLines.count { 
            it.contains("←") && it.contains("methodB") 
        }
        
        // This test should FAIL initially to demonstrate the bug
        // B should appear only once, not twice
        assertEquals("methodB should appear exactly once in enter events (currently fails due to duplication bug)", 1, methodBEnterCount)
        assertEquals("methodB should appear exactly once in exit events (currently fails due to duplication bug)", 1, methodBExitCount)
        
        // Expected correct output should be:
        // → DuplicationBugHierarchy.methodB()  (context for C)
        //  → DuplicationBugHierarchy.methodC()  (filtered method)
        //  ← DuplicationBugHierarchy.methodC
        // ← DuplicationBugHierarchy.methodB
        //
        // NOT:
        // → DuplicationBugHierarchy.methodB()  (context for C)  
        //  → DuplicationBugHierarchy.methodB()  (filtered method) - DUPLICATE!
        //   → DuplicationBugHierarchy.methodC()  (filtered method)
        //   ← DuplicationBugHierarchy.methodC
        //  ← DuplicationBugHierarchy.methodB
        // ← DuplicationBugHierarchy.methodB
    }
    
    @Test
    fun testExpectedCorrectOutput() {
        val output = captureOutput {
            val test = DuplicationBugHierarchy()
            test.methodA()
        }
        
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        // After fix, expected output should be exactly 6 lines:
        // - methodA as context for methodB (methodA doesn't match filter)
        // - methodB as filtered method (methodB matches filter)  
        // - methodC as filtered method (methodC matches filter)
        // - corresponding exits
        assertEquals("Should have exactly 6 trace lines after fix", 6, traceLines.size)
        
        // Verify the exact sequence (corrected expectation)
        assertEquals("Should show context enter for methodA", "→ DuplicationBugHierarchy.methodA()", traceLines[0])
        assertEquals("Should show method enter for methodB", " → DuplicationBugHierarchy.methodB()", traceLines[1])
        assertEquals("Should show method enter for methodC", "  → DuplicationBugHierarchy.methodC()", traceLines[2])
        assertEquals("Should show method exit for methodC", "  ← DuplicationBugHierarchy.methodC", traceLines[3])
        assertEquals("Should show method exit for methodB", " ← DuplicationBugHierarchy.methodB", traceLines[4])
        assertEquals("Should show context exit for methodA", "← DuplicationBugHierarchy.methodA", traceLines[5])
        
        // Critical: Verify NO duplication of methodB
        val methodBEnterCount = traceLines.count { 
            it.contains("→") && it.contains("methodB") 
        }
        assertEquals("methodB should appear exactly once in enter events (no duplication)", 1, methodBEnterCount)
        
        // Verify indentation levels are correct 
        assertTrue("methodA context should have no indentation", traceLines[0].startsWith("→"))
        assertTrue("methodB should be indented 1 level", traceLines[1].startsWith(" →"))
        assertTrue("methodC should be indented 2 levels", traceLines[2].startsWith("  →"))
    }
}

class DuplicationBugHierarchy {
    fun methodA() {
        println("Executing methodA")
        methodB()
    }
    
    fun methodB() {
        println("Executing methodB")
        methodC()
    }
    
    fun methodC() {
        println("Executing methodC")
    }
}