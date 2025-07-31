package io.github.takahirom.codepathtracer

import org.junit.Rule
import org.junit.Test

class BuilderPatternTest {
    
    // Test new Builder pattern with asJUnitRule()
    @get:Rule
    val tracerRule = CodePathTracer.Builder()
        .filter { event ->
            event.className == "io.github.takahirom.codepathtracer.TestCalculator"
        }
        .formatter { event -> 
            "BUILDER: ${event.className}.${event.methodName}()"
        }
        .asJUnitRule()
    
    @Test
    fun testBuilderPatternWithRule() {
        println("Testing new Builder pattern with JUnit Rule...")
        
        val calculator = TestCalculator()
        val result = calculator.add(10, 5)
        println("Builder pattern result: $result")
    }
    
    @Test
    fun testDefaultTracer() {
        println("Testing default tracer...")
        
        // Test default tracer (simple usage)
        codePathTrace {
            val calculator = TestCalculator()
            calculator.multiply(3, 4)
        }
    }
    
    @Test
    fun testCustomTracer() {
        println("Testing custom tracer...")
        
        // Test custom tracer
        val customTracer = CodePathTracer.Builder()
            .filter { event -> 
                event.className == "io.github.takahirom.codepathtracer.TestCalculator"
            }
            .formatter { event -> 
                "CUSTOM: ${event.methodName}() in ${event.className}"
            }
            .build()
            
        codePathTrace(customTracer) {
            val calculator = TestCalculator()
            calculator.add(7, 8)
        }
    }
    
    @Test
    fun testNewBuilderMethod() {
        println("Testing newBuilder() method...")
        
        // Create base tracer
        val baseTracer = CodePathTracer.Builder()
            .formatter { event -> "BASE: ${event.methodName}()" }
            .build()
            
        // Create variation using newBuilder()
        val enhancedTracer = baseTracer.newBuilder()
            .filter { event -> 
                event.className == "io.github.takahirom.codepathtracer.TestCalculator"
            }
            .build()
            
        codePathTrace(enhancedTracer) {
            val calculator = TestCalculator()
            calculator.multiply(6, 7)
        }
    }
}