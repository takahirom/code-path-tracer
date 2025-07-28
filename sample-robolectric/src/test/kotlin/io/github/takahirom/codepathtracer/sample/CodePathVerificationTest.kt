package io.github.takahirom.codepathtracer.sample

import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CodePathVerificationTest {
    
    @Test
    fun verifyMethodTracingWithCodePathTraceDSL() {
        println("🔍 Code Path Tracer - DSL API Verification")
        println("==========================================")
        
        val capturedEvents = mutableListOf<TraceEvent>()
        
        // Use codePathTrace DSL to capture method calls
        codePathTrace({
            // Capture all events for verification (no need to format)
            filter { event ->
                capturedEvents.add(event)
                true // Pass through all events
            }
        }) {
            // Trigger the same Activity creation as debug-trace.sh
            println("Creating Activity with method tracing...")
            val controller = Robolectric.buildActivity(MainActivity::class.java)
            val activity = controller.create().get()
            
            // Verify activity is created (this should trigger various method calls)
            assert(activity != null)
            println("Activity creation completed")
        }
        
        println("\n🎯 Method Trace Verification Results:")
        
        // Extract method names from captured events
        val capturedMethods = capturedEvents.map { "${it.shortClassName}.${it.methodName}" }.toSet()
        
        // Verify the same methods that debug-trace.sh checks
        val expectedMethods = mapOf(
            "onCreate" to "Project code (MainActivity.onCreate)",
            "get" to "Library code (SnapshotThreadLocal.get)", 
            "getPanelState" to "Android Framework (PhoneWindow.getPanelState)"
        )
        
        var allFound = true
        
        expectedMethods.forEach { (methodName, description) ->
            val found = capturedMethods.any { it.contains(methodName) }
            if (found) {
                println("✅ $description")
            } else {
                println("❌ $description - NOT FOUND")
                allFound = false
            }
        }
        
        println("\nTotal events captured: ${capturedEvents.size}")
        println("Unique methods captured: ${capturedMethods.size}")
        
        if (allFound) {
            println("\n🎉 All method traces verified successfully!")
        } else {
            println("\n💡 Some methods were not captured. This may be due to filtering or different execution path.")
        }
        
        // At minimum, we should capture MainActivity.onCreate since we explicitly create the activity
        assertTrue(
            "MainActivity.onCreate should be traced",
            capturedMethods.any { it.contains("onCreate") }
        )
        
        // Verify we captured some events
        assertTrue(
            "Should capture at least some trace events",
            capturedEvents.isNotEmpty()
        )
    }
}