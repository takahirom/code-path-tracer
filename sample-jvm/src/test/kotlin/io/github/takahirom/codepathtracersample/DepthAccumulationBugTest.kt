package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.DefaultFormatter
import org.junit.Rule
import org.junit.Test

/**
 * Test for depth accumulation bug where depth values grow to very large numbers
 * when using beforeContextSize > 0 with filter.
 */
class DepthAccumulationBugTest {
    
    private val capturedDepths = mutableListOf<Int>()
    
    @get:Rule
    val tracerWithBug = CodePathTracer.Builder()
        .beforeContextSize(3)
        .filter { event ->
            // Only trace TestCalculator methods
            event.className.contains("TestCalculator")
        }
        .formatter { event ->
            // Capture depth values for assertion
            capturedDepths.add(event.depth)
            DefaultFormatter.format(event)
        }
        .asJUnitRule()
    
    @Test
    fun testDepthShouldNotAccumulate() {
        capturedDepths.clear()
        
        val testCalculator = TestCalculator("BugTest")
        val fibCalculator = FibonacciTestCalculator()
        
        // Execute many recursive calls mixed with traced calls to trigger accumulation
        repeat(50) { i ->
            // Deep recursive calls that WILL pass the filter and create deep call stacks
            fibCalculator.fibonacci(8 + (i % 3)) // Creates deep recursion with tracing
            
            // This call WILL also pass the filter - depth should NOT accumulate from previous calls
            testCalculator.add(i, 1)
        }
        
        // Ensure we actually captured depth data before checking limits
        assert(capturedDepths.isNotEmpty()) { 
            "No depth data was captured - check your tracing filter configuration" 
        }
        
        // The bug: depth should stay reasonable (< 20), not accumulate to 1000+
        // If the bug exists, later calls will have much higher depth values
        assert(capturedDepths.all { it < 100 }) { 
            "Depth values should not exceed 100, but found: ${capturedDepths.filter { it >= 100 }}" 
        }
        
        // Check if depth is growing unreasonably over time (sign of accumulation bug)
        val firstHalfAvg = capturedDepths.take(capturedDepths.size / 2).average()
        val secondHalfAvg = capturedDepths.drop(capturedDepths.size / 2).average()
        
        
        // If there's a bug, second half will be much larger than first
        val depthGrowth = secondHalfAvg - firstHalfAvg
        assert(depthGrowth < 50) { 
            "Depth should not grow significantly over time. Growth: $depthGrowth (first: $firstHalfAvg, second: $secondHalfAvg)" 
        }
    }
}

// Fibonacci calculator that WILL pass the TestCalculator filter (contains "TestCalculator" in name)
class FibonacciTestCalculator {
    fun fibonacci(n: Int): Long {
        return if (n <= 1) n.toLong() else fibonacci(n - 1) + fibonacci(n - 2)
    }
}

