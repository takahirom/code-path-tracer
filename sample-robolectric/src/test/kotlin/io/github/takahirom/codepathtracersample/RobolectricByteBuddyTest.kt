package io.github.takahirom.codepathtracersample

import io.github.takahirom.codepathtracer.CodePathTracer
import io.github.takahirom.codepathtracer.TraceEvent
import io.github.takahirom.codepathtracer.codePathTrace
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RobolectricByteBuddyTest {
    
    @Test
    fun testByteBuddyWorksInRobolectric() {
        
        val capturedEvents = mutableListOf<TraceEvent>()
        
        codePathTrace(CodePathTracer.Builder()
            .filter{ event ->
                if (event.className.contains("MainActivity")) {
                    capturedEvents.add(event)
                    println("CAPTURED: ${event.className}.${event.methodName}")
                    true
                } else false
            }.build()
        ) {
            try {
                println("Creating MainActivity instance...")
                val mainActivity = MainActivity()
                
                println("Calling handleButtonClick...")
                mainActivity.handleButtonClick()
            } catch (e: Exception) {
                println("MainActivity creation failed: ${e.message} (expected without Android context)")
            }
        }
        
        assert(capturedEvents.isNotEmpty()) { "ByteBuddy should work in Robolectric" }
    }
    
    @Test
    fun testMainActivityDirectly() {
        
        val capturedEvents = mutableListOf<TraceEvent>()
        val mainActivityEvents = mutableListOf<TraceEvent>()
        
        codePathTrace(CodePathTracer.Config(
            autoRetransform = false,
            filter = { event ->
                println("DEBUG: Event captured - ${event.className}.${event.methodName}")
                capturedEvents.add(event)
                
                if (event.className.contains("MainActivity")) {
                    mainActivityEvents.add(event)
                    println("MAIN_ACTIVITY: ${if (event is TraceEvent.Enter) "→" else "←"} ${event.shortClassName}.${event.methodName}")
                }
                true
            }
        )) {
            try {
                println("Creating MainActivity instance directly...")
                val mainActivity = MainActivity()
                
                println("Calling handleButtonClick...")
                mainActivity.handleButtonClick()
                
                println("MainActivity test completed")
            } catch (e: Exception) {
                println("MainActivity creation failed: ${e.message}")
                println("This is expected without Android context")
            }
        }
        
        assert(mainActivityEvents.isNotEmpty()) { "MainActivity methods should be traceable" }
    }
}