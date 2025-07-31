package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracerRule
import org.junit.Rule
import org.junit.Test

class BeforeContextDemoTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracerRule.builder()
        .filter { event ->
            // Only trace specific target methods to test context
            event.className == "io.github.takahirom.codepathtracersample.BeforeContextDemoTest\$ContextCalculator" &&
            event.methodName == "targetMethod"
        }
        .beforeContextSize(3)  // Show 3 context events before filtered events
        .build()
    
    @Test
    fun testBeforeContextDemo() {
        println("=== Testing beforeContextSize functionality ===")
        
        val calculator = ContextCalculator()
        calculator.chainedCalculation()
        
        println("=== Demo completed ===")
    }
    
    inner class ContextCalculator {
        
        fun chainedCalculation(): Int {
            println("Starting chained calculation")
            val step1 = helperMethod1()       // This will be in context buffer
            val step2 = helperMethod2(step1)  // This will be in context buffer  
            val step3 = helperMethod3(step2)  // This will be in context buffer
            val result = targetMethod(step3)  // This passes filter and should show context
            println("Chained calculation result: $result")
            return result
        }
        
        fun helperMethod1(): Int {
            println("Helper method 1")
            return 10
        }
        
        fun helperMethod2(value: Int): Int {
            println("Helper method 2 with value: $value")
            return value + 5
        }
        
        fun helperMethod3(value: Int): Int {
            println("Helper method 3 with value: $value")
            return value * 2
        }
        
        fun targetMethod(value: Int): Int {
            println("Target method with value: $value")
            return value + 100
        }
    }
}