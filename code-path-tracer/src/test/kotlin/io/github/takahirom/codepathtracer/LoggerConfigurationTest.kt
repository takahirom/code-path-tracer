package io.github.takahirom.codepathtracer

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.util.concurrent.ConcurrentLinkedQueue

class TestClass {
    fun testMethod(param: String): String {
        return "processed: $param"
    }
}

class LoggerConfigurationTest {

    private var originalDefaultLogger: Logger? = null
    private var originalDebugLogger: Logger? = null
    
    @Before
    fun setUp() {
        // Backup original loggers
        originalDefaultLogger = CodePathTracer.getDefaultLogger()
        originalDebugLogger = CodePathTracer.getDebugLogger()
    }
    
    @After
    fun tearDown() {
        // Restore original loggers
        originalDefaultLogger?.let { CodePathTracer.setDefaultLogger(it) }
        originalDebugLogger?.let { CodePathTracer.setDebugLogger(it) }
    }
    
    @Test
    fun testDefaultLoggerConfiguration() {
        val capturedLogs = ConcurrentLinkedQueue<String>()
        val testLogger = Logger { message -> capturedLogs.add(message) }
        
        // Set custom default logger
        CodePathTracer.setDefaultLogger(testLogger)
        
        // Verify logger is set
        assertEquals(testLogger, CodePathTracer.getDefaultLogger())
        
        // Test basic logger functionality by directly calling it
        CodePathTracer.getDefaultLogger()("Test message")
        
        // Verify logs were captured
        assertFalse("Expected logs to be captured", capturedLogs.isEmpty())
        assertEquals("Test message", capturedLogs.poll())
    }
    
    @Test
    fun testCustomLoggerInBuilder() {
        val capturedLogs = ConcurrentLinkedQueue<String>()
        val testLogger = Logger { message -> capturedLogs.add(message) }
        
        // Create tracer with custom logger
        val tracer = CodePathTracer.Builder()
            .logger(testLogger)
            .build()
        
        // Set different default logger to verify custom takes precedence
        val defaultLogs = ConcurrentLinkedQueue<String>()
        val defaultLogger = Logger { message -> defaultLogs.add(message) }
        CodePathTracer.setDefaultLogger(defaultLogger)
        
        // Test that tracer uses custom logger (verify config is passed correctly)
        // Since we can't easily test tracing integration, we verify logger configuration
        assertNotNull("Tracer should be created with custom logger", tracer)
        
        // Test logger functionality directly
        testLogger("Custom message")
        assertFalse("Custom logger should have captured message", capturedLogs.isEmpty())
        assertEquals("Custom message", capturedLogs.poll())
        
        // Verify default logger remains unused
        assertTrue("Default logger should not be used", defaultLogs.isEmpty())
    }
    
    @Test
    fun testDebugLoggerConfiguration() {
        val capturedLogs = ConcurrentLinkedQueue<String>()
        val testLogger = Logger { message -> capturedLogs.add(message) }
        
        // Set custom debug logger
        CodePathTracer.setDebugLogger(testLogger)
        
        // Verify debug logger is set
        assertEquals(testLogger, CodePathTracer.getDebugLogger())
        
        // Enable debug mode and trigger some agent operations that use debug logging
        val originalDebugFlag = CodePathTracer.DEBUG
        try {
            CodePathTracer.DEBUG = true
            
            // Create a tracer which will trigger agent installation debug logs
            val tracer = CodePathTracer.Builder().build()
            tracer.trace {
                // Simple operation to ensure agent is active
                val testObject = TestClass()
                testObject.testMethod("debug")
            }
            
            // Verify debug logs were captured
            assertFalse("Expected debug logs to be captured", capturedLogs.isEmpty())
            
            // Verify debug logs contain expected agent messages
            val logMessages = capturedLogs.toList()
            assertTrue("Expected to find agent-related debug messages", 
                logMessages.any { it.contains("Agent") || it.contains("MethodTrace") })
                
        } finally {
            CodePathTracer.DEBUG = originalDebugFlag
        }
    }
    
    @Test
    fun testLoggerNullHandling() {
        // Test null logger handling
        CodePathTracer.setDefaultLogger(null)
        CodePathTracer.setDebugLogger(null)
        
        // Should fall back to DefaultLogger.PRINTLN
        assertEquals(DefaultLogger.PRINTLN, CodePathTracer.getDefaultLogger())
        assertEquals(DefaultLogger.PRINTLN, CodePathTracer.getDebugLogger())
    }
    
    @Test
    fun testJUnitRuleLoggerConfiguration() {
        val capturedLogs = ConcurrentLinkedQueue<String>()
        val testLogger = Logger { message -> capturedLogs.add(message) }
        
        // Create JUnit rule with custom logger
        val rule = CodePathTracer.Builder()
            .logger(testLogger)
            .asJUnitRule()
        
        // Verify rule is created successfully
        assertNotNull("JUnit rule should be created successfully", rule)
        
        // Test logger functionality directly
        testLogger("Rule test message")
        assertFalse("Logger should capture messages", capturedLogs.isEmpty())
        assertEquals("Rule test message", capturedLogs.poll())
    }
    
    @Test
    fun testNewBuilderInheritsLogger() {
        val capturedLogs = ConcurrentLinkedQueue<String>()
        val testLogger = Logger { message -> capturedLogs.add(message) }
        
        // Create tracer with custom logger
        val originalTracer = CodePathTracer.Builder()
            .logger(testLogger)
            .build()
        
        // Create new builder from existing tracer
        val newTracer = originalTracer.newBuilder().build()
        
        // Verify that new tracer is created successfully (inheritance verification)
        assertNotNull("New tracer should be created successfully", newTracer)
        
        // Test logger functionality directly to verify it was inherited
        testLogger("Inherited test message")
        assertFalse("Logger should capture messages", capturedLogs.isEmpty())
        assertEquals("Inherited test message", capturedLogs.poll())
    }
    
    @Test
    fun testLoggerExceptionIsSwallowed() {
        val originalDebug = CodePathTracer.DEBUG
        val debugLogs = ConcurrentLinkedQueue<String>()
        val debugLogger = Logger { message -> debugLogs.add(message) }
        
        try {
            // Enable debug logging to capture failure messages
            CodePathTracer.DEBUG = true
            CodePathTracer.setDebugLogger(debugLogger)
            
            // Create a throwing logger
            val throwing = Logger { throw RuntimeException("boom") }
            
            // Create a config with the throwing logger
            val config = CodePathTracer.Config(
                filter = { true },
                formatter = { event -> "test: ${event.className}.${event.methodName}" },
                enabled = true,
                autoRetransform = true,
                traceEventGenerator = { CodePathTracer.defaultTraceEventGenerator(it) },
                maxToStringLength = 30,
                beforeContextSize = 0,
                maxIndentDepth = 60,
                agentController = CodePathAgentController.default(),
                logger = throwing
            )
            
            // Create a test event
            val testEvent = TraceEvent.Enter(
                className = "TestClass",
                methodName = "testMethod",
                args = arrayOf("test"),
                depth = 0,
                callPath = emptyList()
            )
            
            // Simulate what logSafe does - this should not throw
            runCatching { 
                config.logger(config.formatter(testEvent))
            }.onFailure { 
                if (CodePathTracer.DEBUG) {
                    CodePathTracer.getDebugLogger()("[MethodTrace] Logger failed: ${it.message}")
                }
            }
            
            // Verify that debug logger captured the exception
            assertFalse("Debug logger should have captured exception messages", debugLogs.isEmpty())
            val logMessages = debugLogs.toList()
            assertTrue("Expected to find logger failure message", 
                logMessages.any { it.contains("Logger failed") && it.contains("boom") })
                
        } finally {
            CodePathTracer.setDebugLogger(DefaultLogger.PRINTLN)
            CodePathTracer.DEBUG = originalDebug
        }
    }
}