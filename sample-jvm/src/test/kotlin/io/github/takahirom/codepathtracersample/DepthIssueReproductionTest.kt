package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Test to reproduce depth consistency issues with heavy nesting and filtering
 */
class DepthIssueReproductionTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace specific methods to create filter mismatch scenario
            // This will cause many methods to increment depth but not be logged
            event.className.contains("DeepNesting") && event.methodName == "targetMethod"
        }
        .asJUnitRule()
    
    @Test
    fun testDepthAccumulationWithManyFilteredMethods() {
        println("=== Testing depth accumulation with heavy filtering ===")
        
        val deepNesting = DeepNestingClass()
        
        // Call methods that create deep call stacks but most are filtered out
        repeat(5) { i ->
            println("Iteration $i:")
            deepNesting.startDeepNesting(i)
        }
        
        println("=== Test completed - check depth numbers in output ===")
    }
}

class DeepNestingClass {
    
    fun startDeepNesting(iteration: Int) {
        // Create deep nesting - most methods will be filtered out
        level1(iteration, 0)
    }
    
    fun level1(iteration: Int, depth: Int) {
        helperMethod1(iteration, depth)
        level2(iteration, depth + 1)
        helperMethod2(iteration, depth)
    }
    
    fun level2(iteration: Int, depth: Int) {
        utilityMethod1(iteration, depth)
        level3(iteration, depth + 1)
        utilityMethod2(iteration, depth)
    }
    
    fun level3(iteration: Int, depth: Int) {
        processingMethod1(iteration, depth)
        level4(iteration, depth + 1)
        processingMethod2(iteration, depth)
    }
    
    fun level4(iteration: Int, depth: Int) {
        dataMethod1(iteration, depth)
        level5(iteration, depth + 1)
        dataMethod2(iteration, depth)
    }
    
    fun level5(iteration: Int, depth: Int) {
        computationMethod1(iteration, depth)
        level6(iteration, depth + 1)
        computationMethod2(iteration, depth)
    }
    
    fun level6(iteration: Int, depth: Int) {
        serviceMethod1(iteration, depth)
        finalLevel(iteration, depth + 1)
        serviceMethod2(iteration, depth)
    }
    
    fun finalLevel(iteration: Int, depth: Int) {
        businessLogic1(iteration, depth)
        businessLogic2(iteration, depth)
        // This is the only method that should be traced
        if (iteration == 2) {
            targetMethod(iteration, depth)
        }
        businessLogic3(iteration, depth)
    }
    
    // Only this method passes the filter
    fun targetMethod(iteration: Int, depth: Int) {
        println("Target method called: iteration=$iteration, expected_depth=$depth")
    }
    
    // All these helper methods will be filtered out but still contribute to depth
    fun helperMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun helperMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun utilityMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun utilityMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun processingMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun processingMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun dataMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun dataMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun computationMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun computationMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun serviceMethod1(iteration: Int, depth: Int) { /* filtered */ }
    fun serviceMethod2(iteration: Int, depth: Int) { /* filtered */ }
    fun businessLogic1(iteration: Int, depth: Int) { /* filtered */ }
    fun businessLogic2(iteration: Int, depth: Int) { /* filtered */ }
    fun businessLogic3(iteration: Int, depth: Int) { /* filtered */ }
}