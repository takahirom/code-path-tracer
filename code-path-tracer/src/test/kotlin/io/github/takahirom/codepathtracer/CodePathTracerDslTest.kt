package io.github.takahirom.codepathtracer

import org.junit.Test

class CodePathTracerDslTest {
    
    @Test
    fun testDslApiBasic() {
        println("=== Testing CodePathTracer DSL API ===")
        
        // Test basic DSL usage
        val result = codePathTrace {
            println("Inside traced block")
            "Hello World"
        }
        
        println("Result: $result")
        assert(result == "Hello World")
    }
    
    @Test
    fun testDslApiWithConfig() {
        println("=== Testing CodePathTracer DSL with Config ===")
        
        val config = CodePathTracer.Config(
            filter = { event -> event.className.contains("CodePathTracer") },
            formatter = { event -> "TRACED: ${event.className}.${event.methodName}" }
        )
        
        val result = codePathTrace(config) {
            println("Inside configured traced block")
            calculateSomething(42)
        }
        
        println("Result: $result")
        assert(result == 84)
    }
    
    @Test
    fun testDslApiWithBuilder() {
        println("=== Testing CodePathTracer DSL with Builder ===")
        
        val result = codePathTrace({
            filter { event -> event.depth < 3 }
            formatter { event -> "${" ".repeat(event.depth)}-> ${event.methodName}" }
            enabled(true)
        }) {
            println("Inside builder-configured traced block")
            nestedFunction("test")
        }
        
        println("Result: $result")
        assert(result == "processed: test")
    }
    
    private fun calculateSomething(input: Int): Int {
        return input * 2
    }
    
    private fun nestedFunction(input: String): String {
        return processString(input)
    }
    
    private fun processString(str: String): String {
        return "processed: $str"
    }
}