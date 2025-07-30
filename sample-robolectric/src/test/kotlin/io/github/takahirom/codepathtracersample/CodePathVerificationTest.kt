package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
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
        
        println("\nTotal events captured: ${capturedEvents.size}")
        
        // Verify one event from each category
        assert(applicationEvents.isNotEmpty()) { "Expected application events" }
        assert(frameworkEvents.isNotEmpty()) { "Expected framework events" }
        assert(applicationEvents.any { it.className.contains("MainActivity") }) { 
            "Expected MainActivity events" 
        }
        
        println("Verification completed - traces are working!")
    }
    
    @Test
    fun verifyClickEventTracing() {
        println("🖱️ Click Event Tracing Verification")
        println("===================================")
        
        val capturedEvents = mutableListOf<TraceEvent>()
        
        codePathTrace(CodePathTracer.Config(
            autoRetransform = false,
            filter = { event ->
                if (event.className.contains("MainActivity") && event.methodName == "handleButtonClick") {
                    capturedEvents.add(event)
                    println("${if (event is TraceEvent.Enter) "→" else "←"} ${event.shortClassName}.${event.methodName}")
                    true
                } else false
            }
        )) {
            println("Creating MainActivity directly (not via Robolectric)...")
            try {
                val activity = MainActivity()
                
                println("Calling handleButtonClick directly...")
                activity.handleButtonClick()
                println("Direct handleButtonClick call completed")
            } catch (e: Exception) {
                println("MainActivity creation failed: ${e.message}")
                println("This shows the difference between direct instantiation and Robolectric")
            }
        }
        
        println("\nClick events captured: ${capturedEvents.size}")
        assert(capturedEvents.isNotEmpty()) { "Expected click events" }
        
        println("Click event tracing verification completed!")
    }
}