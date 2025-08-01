package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.DefaultFormatter
import org.junit.Rule
import org.junit.Test

class ConstructorTest {
    
    private val capturedEvents = mutableListOf<String>()
    
    @get:Rule
    val tracerRule = CodePathTracer.Builder()
        .filter { event ->
            // Constructor now shows methodName as "<init>", className is correct
            (event.className.contains("TestClass") && event.methodName == "<init>") || 
            event.className.contains("ConstructorTest")
        }
        .formatter { event ->
            val formatted = DefaultFormatter.format(event)
            capturedEvents.add("Class: '${event.className}' Method: '${event.methodName}' | $formatted")
            formatted
        }
        .asJUnitRule()
    
    @Test
    fun testConstructorMethodName() {
        capturedEvents.clear()
        
        // Create instance to trigger constructor
        val instance = TestClass("hello")
        
        println("Captured events:")
        capturedEvents.forEach { println(it) }
        
        // Check if constructor was captured with correct information
        val constructorEvents = capturedEvents.filter { it.contains("<init>") }
        assert(constructorEvents.isNotEmpty()) { "No constructor events captured" }
        
        // Verify constructor shows correct className and methodName
        val hasCorrectConstructor = capturedEvents.any { 
            it.contains("TestClass") && it.contains("<init>") 
        }
        assert(hasCorrectConstructor) { "Constructor should have correct className and methodName" }
        
        println("Constructor method names found:")
        constructorEvents.forEach { event ->
            println("Event: $event")
        }
    }
}

class TestClass(val value: String) {
    init {
        // Some initialization logic to make sure constructor is traced
        val processed = value.uppercase()
    }
}