package io.github.takahirom.codepathtracer

import org.junit.Rule
import org.junit.Test

class MethodTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracerRule.builder()
        .build()
    
    @Test
    fun testMethodTrace() {
        println("Starting method trace test...")
        
        val calculator = SampleCalculator()
        val result = calculator.add(5, 3)
        println("Result: $result")
        
        val multipliedResult = calculator.multiply(result, 2)
        println("Multiplied result: $multipliedResult")
    }
    
    class SampleCalculator {
        fun add(a: Int, b: Int): Int {
            println("Adding $a + $b")
            return a + b
        }
        
        fun multiply(a: Int, b: Int): Int {
            println("Multiplying $a * $b")
            return a * b
        }
    }
}