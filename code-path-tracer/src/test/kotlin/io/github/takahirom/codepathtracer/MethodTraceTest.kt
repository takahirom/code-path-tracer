package io.github.takahirom.codepathtracer

import org.junit.Rule
import org.junit.Test

class MethodTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event ->
            // Only trace TestCalculator class, avoid inner classes and lambdas
            event.className == "io.github.takahirom.codepathtracer.TestCalculator"
        }
        .asJUnitRule()
    
    @Test
    fun testMethodTrace() {
        println("Starting method trace test...")
        
        val calculator = TestCalculator()
        val result = calculator.add(5, 3)
        println("Result: $result")
        
        val multipliedResult = calculator.multiply(result, 2)
        println("Multiplied result: $multipliedResult")
    }
}