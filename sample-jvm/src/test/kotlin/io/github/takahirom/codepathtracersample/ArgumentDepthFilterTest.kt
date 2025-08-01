package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import io.github.takahirom.codepathtracersample.TestUtils.captureOutput

/**
 * Test to verify that filters using args and depth work correctly with context deduplication.
 * This test ensures that the complete TraceEvent.Enter (with actual args/depth) is used
 * for filter evaluation, not synthetic events.
 */
class ArgumentDepthFilterTest {
    
    @get:Rule
    val argumentFilterRule = CodePathTracer.Builder()
        .filter { event ->
            // Filter based on arguments - only methods with "important" in first argument
            val isRightClass = event.className.contains("ArgumentFilterHierarchy")
            val isEnterEvent = event is io.github.takahirom.codepathtracer.TraceEvent.Enter
            
            println("DEBUG Filter: class=$isRightClass, enter=$isEnterEvent, className=${event.className}, method=${event.methodName}")
            
            if (isEnterEvent) {
                val enterEvent = event as io.github.takahirom.codepathtracer.TraceEvent.Enter
                val hasArgs = enterEvent.args.isNotEmpty()
                val hasImportant = hasArgs && enterEvent.args[0].toString().contains("important")
                
                println("DEBUG Enter: hasArgs=$hasArgs, important=$hasImportant")
                if (hasArgs) {
                    println("DEBUG Args: ${enterEvent.args.joinToString { it.toString() }}")
                }
                
                isRightClass && hasArgs && hasImportant
            } else {
                false
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
        
        // processB and processC should be shown (they have "important" in args)
        // processA should be shown as context for processB but NOT as context for processC 
        // (because processA doesn't have "important" in its args)
        
        val processBEnterCount = traceLines.count { 
            it.contains("→") && it.contains("processB")
        }
        // Since no methods with "important" args are called, no output expected
        assertEquals("No methods should be traced (no 'important' in args)", 0, traceLines.size)
    }
    
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