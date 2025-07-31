package io.github.takahirom.codepathtracer

import org.junit.Test

class BeforeContextTest {
    
    @Test
    fun testBeforeContextWithDSL() {
        // Use DSL approach instead of Rule to avoid ClassCircularityError
        val config = CodePathTracer.Config(
            filter = { event ->
                // Only trace specific methods to test context
                event.className == "io.github.takahirom.codepathtracer.TestContextCalculator" &&
                (event.methodName == "targetMethod" || event.methodName == "anotherTargetMethod")
            },
            beforeContextSize = 2  // Show 2 context events before filtered events
        )
        
        codePathTrace(config) {
            val calculator = TestContextCalculator()
            
            // This should create several method calls, but only some will pass the filter
            calculator.helperMethod1()  // filtered out
            calculator.helperMethod2()  // filtered out  
            calculator.targetMethod()   // passes filter - should show 2 context events
            
            calculator.helperMethod3()  // filtered out
            calculator.anotherTargetMethod()  // passes filter - should show context
        }
        
        // This test is mainly to verify no exceptions are thrown
        // Manual verification would be needed to see the actual output
        println("Test completed successfully - check console output for [context] messages")
    }
}

/**
 * Test class to demonstrate before context functionality
 */
class TestContextCalculator {
    
    fun helperMethod1(): Int {
        return 1
    }
    
    fun helperMethod2(): Int {
        return helperMethod1() + 1
    }
    
    fun targetMethod(): Int {
        return helperMethod2() + 10  // This should trigger context display
    }
    
    fun helperMethod3(): Int {
        return 3
    }
    
    fun anotherTargetMethod(): Int {
        return helperMethod3() + 20  // This should also trigger context display
    }
}