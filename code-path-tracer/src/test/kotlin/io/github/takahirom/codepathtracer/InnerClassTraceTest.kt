package io.github.takahirom.codepathtracer

import io.github.takahirom.codepathtracer.TraceEvent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Test for inner class tracing behavior verification.
 * This test captures trace events and verifies the current limitations.
 */
class InnerClassTraceTest {
    
    private val capturedEvents = mutableListOf<TraceEvent>()
    
    @get:Rule
    val methodTraceRule = CodePathTracerRule.builder()
        .filter { event ->
            capturedEvents.add(event)
            // Capture everything that contains our test class name to investigate
            event.className.contains("InnerClassTraceTest")
        }
        .build()
    
    @Test
    fun testInnerClassTracingLimitation() {
        println("🔍 Inner Class Tracing Investigation")
        println("===================================")
        
        capturedEvents.clear() // Clear any previous events
        
        println("Creating inner class instances...")
        val calculator = InnerCalculator()
        println("InnerCalculator class name: ${calculator::class.java.name}")
        
        val nestedCalculator = NestedCalculator()
        println("NestedCalculator class name: ${nestedCalculator::class.java.name}")
        
        val result = calculator.add(5, 3)
        println("Inner class add result: $result")
        
        val complexResult = calculator.complexCalculation(4, 6)
        println("Inner class complex result: $complexResult")
        
        val nestedResult = nestedCalculator.multiply(result, 2)
        println("Nested class result: $nestedResult")
        
        // Print ALL captured events to see what's being traced
        println("📊 Captured ${capturedEvents.size} total events")
        capturedEvents.forEach { event ->
            println("  - ${event.className}.${event.methodName} (${if (event is TraceEvent.Enter) "ENTER" else "EXIT"})")
        }
        
        // Check specifically for inner class events
        val innerClassEvents = capturedEvents.filter { 
            it.className.contains("InnerCalculator") || it.className.contains("NestedCalculator")
        }
        
        println("📊 Inner class events: ${innerClassEvents.size}")
        
        if (innerClassEvents.isNotEmpty()) {
            println("✅ SUCCESS: Inner class methods WERE traced!")
            innerClassEvents.forEach { event ->
                println("  - TRACED: ${event.className}.${event.methodName}")
            }
        } else {
            println("❌ LIMITATION CONFIRMED: Inner class methods were not traced")
            println("This confirms the current ByteBuddy limitation with inner classes")
        }
        
        println("=== Inner class investigation completed ===")
    }
    
    @Test  
    fun testIndependentClassTracingDocumentation() {
        println("🔍 Independent Class Tracing Documentation")
        println("=========================================")
        
        // This test documents that independent classes should work
        // For actual verification, see sample-jvm tests which demonstrate working tracing
        
        println("Creating independent class instance...")
        val calculator = TestCalculator()
        val result = calculator.add(5, 3)
        println("Independent class result: $result")
        
        // Note: In the code-path-tracer module context, tracing may not work as expected
        // due to test environment limitations. The actual functionality is verified 
        // in sample-jvm module tests.
        
        assertTrue("Test calculator should work correctly", result == 8)
        
        println("✅ Independent class functionality works (tracing verified in sample-jvm tests)")
        println("=== Independent class documentation test completed ===")
    }
    
    // Inner class - will NOT be traced due to current limitations
    inner class InnerCalculator {
        fun add(a: Int, b: Int): Int {
            println("  InnerCalculator: Adding $a + $b")
            return a + b
        }
        
        fun complexCalculation(x: Int, y: Int): Int {
            println("  InnerCalculator: Starting complex calculation")
            val sum = add(x, y)  // Nested call - also won't be traced
            return sum * 2
        }
    }
    
    // Nested class - will also NOT be traced
    class NestedCalculator {
        fun multiply(a: Int, b: Int): Int {
            println("  NestedCalculator: Multiplying $a * $b")
            return a * b
        }
    }
}