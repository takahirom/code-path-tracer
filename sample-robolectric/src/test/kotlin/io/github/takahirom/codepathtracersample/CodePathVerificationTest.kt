package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
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
        val applicationEvents = mutableListOf<TraceEvent>()
        val frameworkEvents = mutableListOf<TraceEvent>()
        val libraryEvents = mutableListOf<TraceEvent>()
        
        codePathTrace({
            filter { event ->
                capturedEvents.add(event)
                
                when {
                    event.className.startsWith("io.github.takahirom.codepathtracersample") -> {
                        applicationEvents.add(event)
                    }
                    event.className.startsWith("android.view") || event.className.startsWith("android.widget") -> {
                        frameworkEvents.add(event)
                    }
                    event.className.startsWith("androidx.") || event.className.startsWith("kotlin.") -> {
                        libraryEvents.add(event)
                    }
                }
                true
            }
        }) {
            println("Creating Activity with method tracing...")
            val controller = Robolectric.buildActivity(MainActivity::class.java)
            val activity = controller.create().get()
            
            // Try to trigger some view operations
            activity.findViewById<android.widget.Button>(activity.getButtonId())?.let { button ->
                println("Button found, performing click...")
                button.performClick()
            }
            
            assert(activity != null)
            println("Activity creation completed")
        }
        
        println("\n=== Event Summary ===")
        println("Total events captured: ${capturedEvents.size}")
        println("Application events: ${applicationEvents.size}")
        println("Framework events: ${frameworkEvents.size}")
        println("Library events: ${libraryEvents.size}")
        
        // Verify that we captured at least one event from each category
        assert(applicationEvents.isNotEmpty()) { "Expected to capture at least one application event" }
        assert(frameworkEvents.isNotEmpty()) { "Expected to capture at least one framework event" }
        
        // Verify specific application classes are traced
        assert(applicationEvents.any { it.className.contains("MainActivity") }) { 
            "Expected to capture MainActivity events" 
        }
        assert(applicationEvents.any { it.className.contains("CodePathVerificationTest") }) { 
            "Expected to capture test class events" 
        }
        
        // Verify specific framework classes are traced
        assert(frameworkEvents.any { it.className.startsWith("android.view.View") }) { 
            "Expected to capture android.view.View events" 
        }
        assert(frameworkEvents.any { it.className.startsWith("android.widget.") }) { 
            "Expected to capture android.widget events" 
        }
        
        // Verify library events if present
        if (libraryEvents.isNotEmpty()) {
            assert(libraryEvents.any { it.className.startsWith("androidx.") }) { 
                "Expected androidx events if library events exist" 
            }
        }
        
        // Verify total event count is reasonable
        assert(capturedEvents.size > 100) { 
            "Expected substantial number of events, got ${capturedEvents.size}" 
        }
        
        // Verify event distribution is reasonable
        val totalEvents = applicationEvents.size + frameworkEvents.size + libraryEvents.size
        assert(totalEvents > 0) { "No categorized events found" }
        assert(frameworkEvents.size > applicationEvents.size) { 
            "Expected more framework events than application events" 
        }
        
        println("Verification completed - traces are working!")
    }
    
    @Test
    fun verifyClickEventTracing() {
        println("🖱️ Click Event Tracing Verification")
        println("===================================")
        
        val capturedEvents = mutableListOf<TraceEvent>()
        
        println("Creating Activity...")
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()
        
        try {
            println("\n🎯 Simulating button click with tracing...")
            
            codePathTrace({
                filter { event ->
                    if (event.className.contains("MainActivity")) {
                        capturedEvents.add(event)
                        println("${if (event is TraceEvent.Enter) "→" else "←"} ${event.shortClassName}.${event.methodName}")
                        true
                    } else false
                }
            }) {
                val button = activity.findViewById<android.widget.Button>(activity.getButtonId())
                button.performClick()
                println("Button click simulation completed")
            }
        } finally {
            controller.pause().stop().destroy()
        }
        
        println("\nTotal click-related events captured: ${capturedEvents.size}")
        
        // Verify click events were captured
        assert(capturedEvents.isNotEmpty()) { "Expected to capture click-related events" }
        assert(capturedEvents.all { it.className.contains("MainActivity") }) { 
            "All captured events should be from MainActivity" 
        }
        
        // Verify we have the expected event types (at least one should be captured)
        val enterEvents = capturedEvents.filterIsInstance<TraceEvent.Enter>()
        val exitEvents = capturedEvents.filterIsInstance<TraceEvent.Exit>()
        assert(enterEvents.isNotEmpty() || exitEvents.isNotEmpty()) { 
            "Expected at least one Enter or Exit event" 
        }
        
        // Verify event consistency
        assert(capturedEvents.isNotEmpty()) { "Expected at least one event" }
        
        println("Click event tracing verification completed!")
    }
}