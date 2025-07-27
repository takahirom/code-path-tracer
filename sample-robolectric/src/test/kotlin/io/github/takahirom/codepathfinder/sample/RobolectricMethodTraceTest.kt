package io.github.takahirom.codepathfinder.sample

import io.github.takahirom.codepathfinder.MethodTraceRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RobolectricMethodTraceTest {
    
    @get:Rule
    val methodTraceRule = MethodTraceRule.builder()
        .build()
    
    @Test
    fun testActivityCreationWithTrace() {
        println("=== Starting Robolectric Activity Test with Method Trace ===")
        
        // Create Activity Controller
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        
        // Lifecycle: Create
        println("Creating activity...")
        val activity = controller.create().get()
        
        // Verify activity is created
        assert(activity != null)
        println("Activity created successfully")
        
        // Lifecycle: Start
        println("Starting activity...")
        controller.start()
        
        // Lifecycle: Resume
        println("Resuming activity...")
        controller.resume()
        
        println("=== Activity lifecycle test completed ===")
    }
    
    @Test
    fun testBusinessLogicWithTrace() {
        println("=== Testing Business Logic with Method Trace ===")
        println("Agent initialized: ${methodTraceRule}")
        
        val calculator = SampleCalculator()
        
        // Test addition - should show method trace
        println("Calling calculator.add(10, 5)...")
        val result1 = calculator.add(10, 5)
        println("Addition result: $result1")
        assert(result1 == 15)
        
        // Test multiplication - should show method trace  
        println("Calling calculator.multiply($result1, 2)...")
        val result2 = calculator.multiply(result1, 2)
        println("Multiplication result: $result2")
        assert(result2 == 30)
        
        // Test complex calculation - should show nested method traces
        println("Calling calculator.complexCalculation(5, 3)...")
        val result3 = calculator.complexCalculation(5, 3)
        println("Complex calculation result: $result3")
        assert(result3 == 28) // (5 + 3) * 2 + 12
        
        println("=== Business logic test completed ===")
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
}