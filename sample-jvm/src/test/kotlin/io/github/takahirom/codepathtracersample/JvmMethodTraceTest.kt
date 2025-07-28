package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.CodePathTracerAgent
import io.github.takahirom.codepathtracer.CodePathTracerRule
import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
import org.junit.Rule
import org.junit.Test

class JvmMethodTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracerRule.builder()
        .filter { event -> 
            // Trace everything with sample in name, including inner class style names
            event.className.contains("Sample") || 
            event.className.contains("sample") ||
            event.className.contains("InnerClassStyle") ||
            event.className.contains("TestClass$")
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
        
        val calculator = TestCalculator("BusinessCalculator")
        
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
    fun testInnerClassStyleTracing() {
        println("=== Testing Inner Class Style Tracing ===")
        
        val calculator = InnerClassStyleCalculator()
        val result = calculator.add(5, 3)
        println("InnerClassStyleCalculator result: $result")
        
        val complexResult = calculator.complexCalculation(4, 6)
        println("InnerClassStyleCalculator complex result: $complexResult")
        
        val dollarCalculator = TestClassInnerStyle()
        val multiplyResult = dollarCalculator.multiply(result, 2)
        println("TestClassInnerStyle result: $multiplyResult")
        
        println("=== Inner class style test completed ===")
    }
    
    @Test
    fun testSimpleCodePathTrace() {
        println("=== Testing Simple CodePathTrace DSL ===")
        
        // 先にエージェントを初期化してからクラスをロード
        CodePathTracerAgent.initialize(
            CodePathTracer.simple()
        )
        Class.forName("io.github.takahirom.codepathtracersample.JvmMethodTraceTest\$SampleCalculator")
        
        val calculator = SampleCalculator()
        
        codePathTrace {
            calculator.complexCalculation(5, 3)
        }

        println("=== DSL test completed ===")
    }
    
    @Test
    fun testDepthIndentation() {
        println("=== Testing Depth Indentation ===")
        
        val calculator = TestCalculator("DepthTest")
        val result = calculator.complexCalculation(2, 1)
        println("Final result: $result")
        
        println("=== Depth test completed ===")
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