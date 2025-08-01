package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.CodePathTracerAgent
import io.github.takahirom.codepathtracer.CodePathTracerRule
import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
import org.junit.Rule
import org.junit.Test

class JvmMethodTraceTest {
    
    // Field to capture events for verification
    private val capturedEvents = mutableListOf<TraceEvent>()
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event -> 
            // Capture events for verification
            capturedEvents.add(event)
            
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
        .asJUnitRule()
    
    @Test
    fun testBusinessLogicWithTrace() {
        // Clear any previous events
        capturedEvents.clear()
        
        // Verify agent config is available
        val config = CodePathTracerAgent.getConfig()
        assert(config != null) { "Expected trace config to be available" }
        
        val calculator = TestCalculator("BusinessCalculator")
        
        // Test addition
        val result1 = calculator.add(10, 5)
        assert(result1 == 15) { "Expected addition result to be 15, got $result1" }
        
        // Test multiplication
        val result2 = calculator.multiply(result1, 2)
        assert(result2 == 30) { "Expected multiplication result to be 30, got $result2" }
        
        // Test complex calculation
        val result3 = calculator.complexCalculation(5, 3)
        assert(result3 == 28) { "Expected complex calculation result to be 28, got $result3" } // (5 + 3) * 2 + 12
        
        // Verify tracing is working
        assert(capturedEvents.isNotEmpty()) { "Expected trace events, got ${capturedEvents.size}" }
        
        val calculatorEvents = capturedEvents.filter { it.className.contains("TestCalculator") }
        assert(calculatorEvents.isNotEmpty()) { "Expected TestCalculator events, found ${calculatorEvents.size}" }
    }
    
    @Test
    fun testDataProcessing() {
        val processor = DataProcessor()
        
        val data = listOf("apple", "banana", "cherry")
        val processed = processor.processStrings(data)
        
        assert(processed.size == 3) { "Expected 3 processed items, got ${processed.size}" }
        assert(processed.all { it.startsWith("PROCESSED:") }) { "All items should start with PROCESSED:, got $processed" }
        
        // Verify specific content
        assert(processed.contains("PROCESSED:APPLE")) { "Expected PROCESSED:APPLE in $processed" }
        assert(processed.contains("PROCESSED:BANANA")) { "Expected PROCESSED:BANANA in $processed" }
        assert(processed.contains("PROCESSED:CHERRY")) { "Expected PROCESSED:CHERRY in $processed" }
    }
    
    @Test
    fun testInnerClassStyleTracing() {
        val calculator = InnerClassStyleCalculator()
        val result = calculator.add(5, 3)
        assert(result == 8) { "Expected InnerClassStyleCalculator addition result to be 8, got $result" }
        
        val complexResult = calculator.complexCalculation(4, 6)
        assert(complexResult == 20) { "Expected InnerClassStyleCalculator complex result to be 20, got $complexResult" } // (4 + 6) * 2
        
        val dollarCalculator = TestClassInnerStyle()
        val multiplyResult = dollarCalculator.multiply(result, 2)
        assert(multiplyResult == 16) { "Expected TestClassInnerStyle multiply result to be 16, got $multiplyResult" }
    }
    
    @Test
    fun testSimpleCodePathTrace() {
        // 先にエージェントを初期化してからクラスをロード
        CodePathTracerAgent.initialize(
            CodePathTracer.Config()
        )
        val clazz = Class.forName("io.github.takahirom.codepathtracersample.JvmMethodTraceTest\$SampleCalculator")
        assert(clazz != null) { "Expected SampleCalculator class to be loadable" }
        
        val calculator = SampleCalculator()
        
        val result = codePathTrace {
            calculator.complexCalculation(5, 3)
        }
        
        assert(result == 28) { "Expected DSL trace result to be 28, got $result" } // (5 + 3) * 2 + 12
    }
    
    @Test
    fun testDepthIndentation() {
        val calculator = TestCalculator("DepthTest")
        val result = calculator.complexCalculation(2, 1)
        assert(result == 18) { "Expected depth test result to be 18, got $result" } // (2 + 1) * 2 + 12
    }
    
    class SampleCalculator {
        fun add(a: Int, b: Int): Int {
            return a + b
        }
        
        fun multiply(a: Int, b: Int): Int {
            return a * b
        }
        
        fun complexCalculation(x: Int, y: Int): Int {
            val sum = add(x, y)
            val doubled = multiply(sum, 2)
            val final = add(doubled, 12)
            return final
        }
    }
    
    class DataProcessor {
        fun processStrings(input: List<String>): List<String> {
            return input.map { processString(it) }
        }
        
        private fun processString(str: String): String {
            return "PROCESSED:${str.uppercase()}"
        }
    }
    
    @Test
    fun testCustomTraceEventGenerator() {
        // Use the default traceEventGenerator to confirm the feature is working
        val customTracer = CodePathTracer.Builder()
            .filter { event -> event.className.contains("SampleCalculator") }
            .traceEventGenerator(CodePathTracer::defaultTraceEventGenerator)
            .build()
        
        val result = codePathTrace(customTracer) {
            val calculator = SampleCalculator()
            calculator.add(2, 3)
        }
        
        assert(result == 5) { "Expected custom tracer result to be 5, got $result" }
    }
    
    @Test
    fun testMaxToStringLengthConfig() {
        // Test with very short maxToStringLength
        val shortTracer = CodePathTracer.Builder()
            .filter { event -> event.className.contains("SampleCalculator") }
            .maxToStringLength(5)
            .build()
        
        val result = codePathTrace(shortTracer) {
            val calculator = SampleCalculator()
            calculator.add(123456789, 987654321) // Long numbers should be truncated
        }
        
        assert(result == 1111111110) { "Expected maxToStringLength test result to be 1111111110, got $result" }
    }
}