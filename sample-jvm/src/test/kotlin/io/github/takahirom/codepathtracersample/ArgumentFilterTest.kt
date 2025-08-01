package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

/**
 * Test to verify argument-based filtering with context deduplication.
 */
class ArgumentFilterTest {
    
    @get:Rule
    val argumentFilterRule = CodePathTracer.Builder()
        .filter { event ->
            // Filter based on arguments - only methods with "important" in first argument
            val isRightClass = event.className.contains("ArgumentFilterHierarchy")
            
            when (event) {
                is io.github.takahirom.codepathtracer.TraceEvent.Enter -> {
                    val hasArgs = event.args.isNotEmpty()
                    val hasImportant = hasArgs && event.args[0].toString().contains("important")
                    isRightClass && hasArgs && hasImportant
                }
                is io.github.takahirom.codepathtracer.TraceEvent.Exit -> {
                    // Allow Exit events for the same class to maintain trace completeness
                    isRightClass
                }
            }
        }
        .beforeContextSize(1)
        .asJUnitRule()
    
    @Test
    fun testArgumentBasedFilterWithContext() {
        val output = captureOutput {
            val test = ArgumentFilterHierarchy()
            test.processA("normal-data")      // Should not be filtered
            test.processB("important-data")   // Should be filtered
            test.processC("important-task")   // Should be filtered  
        }
        
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        println("=== Argument filter trace output ===")
        traceLines.forEach { println(it) }
        
        // processB and processC should be traced (they have "important" in their args)
        assertTrue("Methods with 'important' args should be traced", traceLines.isNotEmpty())
        
        val processBCount = traceLines.count { it.contains("processB") }
        val processCCount = traceLines.count { it.contains("processC") }
        assertTrue("processB should be traced", processBCount > 0)
        assertTrue("processC should be traced", processCCount > 0)
    }
}

/**
 * Test to verify depth-based filtering with context deduplication.
 */
class DepthFilterTest {
    
    @get:Rule
    val depthFilterRule = CodePathTracer.Builder()
        .filter { event ->
            // Filter based on depth - only methods at depth >= 2
            event.className.contains("DepthFilterHierarchy") && 
            event.depth >= 2
        }
        .beforeContextSize(2)
        .asJUnitRule()
    
    @Test 
    fun testDepthBasedFilterWithContext() {
        val output = captureOutput {
            val test = DepthFilterHierarchy()
            test.level0()  // depth 0 - should not be filtered
            // -> level1() depth 1 - should not be filtered
            // -> -> level2() depth 2 - should be filtered
            // -> -> -> level3() depth 3 - should be filtered
        }
        
        val traceLines = output.lines()
            .filter { it.contains("→") || it.contains("←") }
        
        println("=== Depth filter trace output ===")
        traceLines.forEach { println(it) }
        
        // level2 and level3 should be shown (depth >= 2)
        // level0 and level1 should be shown as context (depth < 2)
        
        val level2Count = traceLines.count { 
            it.contains("→") && it.contains("level2")
        }
        // level2 appears once as filtered method (no duplication)
        assertEquals("level2 should appear exactly once", 1, level2Count)
        
        val level3Count = traceLines.count { 
            it.contains("→") && it.contains("level3") 
        }
        assertEquals("level3 should appear exactly once", 1, level3Count)
        
        // Check that the depth filter is working - only methods with depth >= 2 should be filtered
        assertTrue("Depth filter should show level2 and level3", level2Count == 1 && level3Count == 1)
    }
}

class ArgumentFilterHierarchy {
    fun processA(data: String) {
        println("Processing A with: $data")
        processB("important-data")
    }
    
    fun processB(data: String) {
        println("Processing B with: $data")
        processC("important-task")
    }
    
    fun processC(data: String) {
        println("Processing C with: $data")
    }
}

class DepthFilterHierarchy {
    fun level0() {
        println("Level 0")
        level1()
    }
    
    fun level1() {
        println("Level 1")
        level2()
    }
    
    fun level2() {
        println("Level 2")
        level3()
    }
    
    fun level3() {
        println("Level 3")
    }
}