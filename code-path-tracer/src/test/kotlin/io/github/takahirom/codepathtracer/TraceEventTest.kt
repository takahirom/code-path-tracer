package io.github.takahirom.codepathtracer

import org.junit.Test
import org.junit.Assert.*

class TraceEventTest {
    
    @Test
    fun testPackageNameCompression() {
        // Basic test - should compress packages with 2+ segments
        assertEquals("c.g.t.MyClass", TraceEvent.safeToString("com.github.takahirom.MyClass", 1000))
        assertEquals("a.a.ComponentActivity", TraceEvent.safeToString("androidx.activity.ComponentActivity", 1000))
        assertEquals("Simple", TraceEvent.safeToString("Simple", 1000))
        assertEquals("c.g.t.MyClass(c.g.t.MyValueClass(1))", TraceEvent.safeToString("com.github.takahirom.MyClass(com.github.takahirom.MyValueClass(1))", 1000))
    }
    
    @Test
    fun testSafeToStringBasicFunctionality() {
        // Test basic functionality still works
        assertEquals("null", TraceEvent.safeToString(null))
        assertEquals("Unit", TraceEvent.safeToString(Unit))
        assertEquals("hello", TraceEvent.safeToString("hello"))
        assertEquals("123", TraceEvent.safeToString(123))
        
        // Test max length truncation
        assertEquals("hello", TraceEvent.safeToString("hello world", 5))
        assertEquals("c.g.t", TraceEvent.safeToString("com.github.takahirom.VeryLongClassName", 5))
    }
    
    @Test
    fun testDefaultFormatting() {
        val enterEvent = TraceEvent.Enter(
            className = "com.example.MyClass",
            methodName = "testMethod",
            args = arrayOf("hello", 123),
            depth = 2
        )
        
        val formatted = enterEvent.defaultFormat()
        // Should have 4 spaces indent (depth 2 * 2) and compressed class name in args
        assertTrue(formatted.startsWith("    → MyClass.testMethod(hello, 123)"))
    }
    
    @Test 
    fun testDeepIndentFormat() {
        val deepEvent = TraceEvent.Enter(
            className = "com.example.DeepClass", 
            methodName = "deepMethod",
            args = emptyArray(),
            depth = 62
        )
        
        val formatted = deepEvent.defaultFormat()
        // Should show depth number when >= 60 (maxIndentDepth)
        assertTrue(formatted.contains("62⇢"))
        assertTrue(formatted.contains("→ DeepClass.deepMethod()"))
    }
}