package io.github.takahirom.codepathtracer.sample

import io.github.takahirom.codepathtracer.CodePathTracerRule
import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
import org.junit.Rule
import org.junit.Test

class JvmMethodTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracerRule.builder()
        .filter { event -> 
            // Use broader filter - trace everything in our test package
            event.className.contains("sample") && 
                             (event.className.contains("Calculator") || 
                              event.className.contains("Processor") ||
                              event.className.contains("JvmMethodTraceTest"))
        }
        .formatter { event -> 
            when (event) {
                is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
                is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
            }
        }
        .build()
    
    @Test
    fun testBusinessLogicWithTrace() {
        println("=== Testing Business Logic with Method Trace ===")
        
        val calculator = SampleCalculator()
        
        // Test addition
        val result1 = calculator.add(10, 5)
        println("Addition result: $result1")
        assert(result1 == 15)
        
        // Test multiplication
        val result2 = calculator.multiply(result1, 2)
        println("Multiplication result: $result2")
        assert(result2 == 30)
        
        // Test complex calculation
        val result3 = calculator.complexCalculation(5, 3)
        println("Complex calculation result: $result3")
        assert(result3 == 28) // (5 + 3) * 2 + 12
        
        println("=== Business logic test completed ===")
    }
    
    @Test
    fun testDataProcessing() {
        println("=== Testing Data Processing with Method Trace ===")
        
        val processor = DataProcessor()
        
        val data = listOf("apple", "banana", "cherry")
        val processed = processor.processStrings(data)
        
        println("Processed data: $processed")
        assert(processed.size == 3)
        assert(processed.all { it.startsWith("PROCESSED:") })
        
        println("=== Data processing test completed ===")
    }
    
    @Test
    fun testSimpleCodePathTrace() {
        println("=== Testing Simple CodePathTrace DSL ===")
        
        val calculator = SampleCalculator()
        
        codePathTrace {
            calculator.complexCalculation(5, 3)
        }
        
        println("=== DSL test completed ===")
    }
    
    class SampleCalculator {
        fun add(a: Int, b: Int): Int {
            println("  Calculator: Adding $a + $b")
            return a + b
        }
        
        fun multiply(a: Int, b: Int): Int {
            println("  Calculator: Multiplying $a * $b")
            return a * b
        }
        
        fun complexCalculation(x: Int, y: Int): Int {
            println("  Calculator: Starting complex calculation with $x and $y")
            val sum = add(x, y)
            val doubled = multiply(sum, 2)
            val final = add(doubled, 12)
            println("  Calculator: Complex calculation complete")
            return final
        }
    }
    
    class DataProcessor {
        fun processStrings(input: List<String>): List<String> {
            println("  Processor: Processing ${input.size} strings")
            return input.map { processString(it) }
        }
        
        private fun processString(str: String): String {
            println("  Processor: Processing single string: $str")
            return "PROCESSED:${str.uppercase()}"
        }
    }
}