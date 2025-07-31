package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.TraceEvent
import org.junit.Rule
import org.junit.Test

class InnerClassInvestigationTest {
    
    private val capturedEvents = mutableListOf<TraceEvent>()
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace inner classes, not the test class itself to avoid infinite recursion
            val shouldTrace = event.className.contains("InnerCalculator") || 
                             event.className.contains("NestedCalculator") ||
                             event.className.contains("NestedProcessor") ||
                             event.className.contains("ExternalClassWithInner")
            if (shouldTrace) {
                capturedEvents.add(event)
            }
            shouldTrace
        }
        .asJUnitRule()
    
    @Test
    fun investigateExternalInnerClassTracing() {
        println("🔍 External Inner Class Tracing Investigation")
        println("============================================")
        
        capturedEvents.clear()
        
        println("Testing external class with inner classes...")
        val external = ExternalClassWithInner()
        
        println("Testing inner class from external class...")
        val innerResult = external.useInnerClass()
        println("External inner class result: $innerResult")
        
        println("Testing nested class from external class...")
        val nestedResult = external.useNestedClass()
        println("External nested class result: $nestedResult")
        
        // Print captured events
        println("📊 Captured ${capturedEvents.size} total events:")
        capturedEvents.forEach { event ->
            println("  - ${event.className}.${event.methodName} (${if (event is TraceEvent.Enter) "ENTER" else "EXIT"})")
        }
        
        // Check for external inner class events
        val externalInnerEvents = capturedEvents.filter { 
            it.className.contains("ExternalClassWithInner")
        }
        
        if (externalInnerEvents.isNotEmpty()) {
            println("✅ SUCCESS: External inner class methods WERE traced!")
            externalInnerEvents.forEach { event ->
                println("  - TRACED: ${event.className}.${event.methodName}")
            }
        } else {
            println("❌ LIMITATION: External inner class methods were not traced either")
        }
        
        println("=== External inner class investigation completed ===")
    }

    @Test
    fun investigateInnerClassTracing() {
        println("🔍 Inner Class Tracing Investigation in sample-jvm")
        println("================================================")
        
        capturedEvents.clear()
        
        println("Creating inner class instance...")
        val calculator = InnerCalculator()
        println("InnerCalculator class name: ${calculator::class.java.name}")
        
        val result = calculator.add(5, 3)
        println("Inner class add result: $result")
        
        val complexResult = calculator.complexCalculation(4, 6)
        println("Inner class complex result: $complexResult")
        
        println("Creating nested class instance...")
        val nestedCalculator = NestedCalculator()
        println("NestedCalculator class name: ${nestedCalculator::class.java.name}")
        
        val nestedResult = nestedCalculator.multiply(result, 2)
        println("Nested class result: $nestedResult")
        
        // Print ALL captured events
        println("📊 Captured ${capturedEvents.size} total events:")
        capturedEvents.forEach { event ->
            println("  - ${event.className}.${event.methodName} (${if (event is TraceEvent.Enter) "ENTER" else "EXIT"})")
        }
        
        // Analyze inner class events
        val innerClassEvents = capturedEvents.filter { 
            it.className.contains("InnerCalculator") || it.className.contains("NestedCalculator")
        }
        
        if (innerClassEvents.isNotEmpty()) {
            println("✅ SUCCESS: Inner class methods WERE traced!")
            innerClassEvents.forEach { event ->
                println("  - TRACED: ${event.className}.${event.methodName}")
            }
        } else {
            println("❌ LIMITATION: Inner class methods were not traced")
        }
        
        println("=== Investigation completed ===")
    }
    
    // Inner class - test if this gets traced
    inner class InnerCalculator {
        fun add(a: Int, b: Int): Int {
            println("  InnerCalculator: Adding $a + $b")
            return a + b
        }
        
        fun complexCalculation(x: Int, y: Int): Int {
            println("  InnerCalculator: Starting complex calculation")
            val sum = add(x, y)  // Nested call
            return sum * 2
        }
    }
    
    // Nested class - test if this gets traced
    class NestedCalculator {
        fun multiply(a: Int, b: Int): Int {
            println("  NestedCalculator: Multiplying $a * $b")
            return a * b
        }
    }
}