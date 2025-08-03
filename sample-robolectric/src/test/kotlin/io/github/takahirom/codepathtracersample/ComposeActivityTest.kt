package io.github.takahirom.codepathtracersample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.takahirom.codepathtracer.CodePathTracer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"])
class ComposeActivityTest {
    
    @get:Rule(order = 0)
    val codePathTracer = CodePathTracer.Builder()
        .filter { event -> 
            event.methodName.contains("calculateNewValue") ||
            event.methodName.contains("Counter")
        }
        .asJUnitRule()
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    @Test
    fun testCounterScreenWithTracing() {
        composeTestRule.setContent {
            CounterScreen()
        }
        
        // Verify initial UI state
        composeTestRule.onNodeWithText("Code Path Tracer Sample")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Count: 0")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Increment")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun testButtonClickWithTracing() {
        composeTestRule.setContent {
            CounterScreen()
        }
        
        // Initial state
        composeTestRule.onNodeWithText("Count: 0")
            .assertExists()
        
        // Click increment button
        composeTestRule.onNodeWithText("Increment")
            .performClick()
        
        // Verify count increased
        composeTestRule.onNodeWithText("Count: 1")
            .assertExists()
        
        // Multiple clicks
        repeat(3) {
            composeTestRule.onNodeWithText("Increment")
                .performClick()
        }
        
        // Verify final count
        composeTestRule.onNodeWithText("Count: 4")
            .assertExists()
    }
    
    @Test
    fun testCalculateNewValueFunction() {
        val result1 = calculateNewValue(0)
        assert(result1 == 1) { "Expected 1, got $result1" }
        
        val result2 = calculateNewValue(10)
        assert(result2 == 11) { "Expected 11, got $result2" }
    }
}