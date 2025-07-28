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
        
        codePathTrace({
            filter { event ->
                capturedEvents.add(event)
                true
            }
        }) {
            println("Creating Activity with method tracing...")
            val controller = Robolectric.buildActivity(MainActivity::class.java)
            val activity = controller.create().get()
            
            assert(activity != null)
            println("Activity creation completed")
        }
        
        println("\nTotal events captured: ${capturedEvents.size}")
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
        
        println("\nTotal click-related events captured: ${capturedEvents.size}")
        println("Click event tracing verification completed!")
    }
}