package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RobolectricMethodTraceTest {
    
    @get:Rule
    val methodTraceRule = CodePathTracer.Builder()
        .filter { event -> event.className.contains("MainActivity") }
        .asJUnitRule()
    
    @Test
    fun testActivityCreationWithTrace() {
        // Create Activity Controller
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        
        try {
            // Lifecycle: Create
            val activity = controller.create().get()
            
            // Verify activity is created
            assert(activity != null) { "Expected activity to be created successfully" }
            
            // Lifecycle: Start
            controller.start()
            
            // Lifecycle: Resume
            controller.resume()
            
            // Verify activity state
            assert(!activity.isFinishing) { "Expected activity to not be finishing" }
        } finally {
            controller.pause().stop().destroy()
        }
    }
    
    @Test
    fun testBusinessLogicWithTrace() {
        // Verify method trace rule is initialized
        assert(methodTraceRule != null) { "Expected methodTraceRule to be initialized" }
        
        val calculator = SampleCalculator()
        
        // Test addition
        val result1 = calculator.add(10, 5)
        assert(result1 == 15) { "Expected addition result to be 15, got $result1" }
        
        // Test multiplication
        val result2 = calculator.multiply(result1, 2)
        assert(result2 == 30) { "Expected multiplication result to be 30, got $result2" }
        
        // Test complex calculation
        val result3 = calculator.complexCalculation(5, 3)
        assert(result3 == 28) { "Expected complex calculation result to be 28, got $result3" } // (5 + 3) * 2 + 12
    }
    

    @Test
    fun testClickEventTracing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()
        
        try {
            val button = activity.findViewById<android.widget.Button>(activity.getButtonId())
            button.performClick()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun testEspressoClickEventTracing() {
        // Use ActivityScenario for proper Espresso integration with Robolectric
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Use proper Espresso onView with text matcher to find and click button
            onView(withText("Increment")).perform(click())
            
            // Verify the test completed successfully (no exceptions thrown)
            assert(true) { "Espresso test should complete without exceptions" }
        }
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