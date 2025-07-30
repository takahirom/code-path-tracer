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
        println("🔍 Testing ByteBuddy in Robolectric environment")
        
        val capturedEvents = mutableListOf<TraceEvent>()
        
        codePathTrace(CodePathTracer.Config(
            autoRetransform = false,
            filter = { event ->
                if (event.className.contains("MainActivity")) {
                    capturedEvents.add(event)
                    println("CAPTURED: ${event.className}.${event.methodName}")
                    true
                } else false
            }
        )) {
            try {
                println("Creating MainActivity instance...")
                val mainActivity = MainActivity()
                
                println("Calling handleButtonClick...")
                mainActivity.handleButtonClick()
            } catch (e: Exception) {
                println("MainActivity creation failed: ${e.message} (expected without Android context)")
            }
        }
        
        println("Total events captured: ${capturedEvents.size}")
        if (capturedEvents.isNotEmpty()) {
            println("✅ ByteBuddy works in Robolectric!")
        } else {
            println("❌ ByteBuddy does NOT work in Robolectric")
        }
    }
    
    @Test
    fun testMainActivityDirectly() {
        println("🔍 Testing MainActivity directly (no Robolectric controller)")
        
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
        
        println("Total events captured: ${capturedEvents.size}")
        println("MainActivity events captured: ${mainActivityEvents.size}")
        
        if (mainActivityEvents.isNotEmpty()) {
            println("✅ MainActivity methods are traceable!")
            mainActivityEvents.forEach { event ->
                println("  - ${event.className}.${event.methodName}")
            }
        } else {
            println("❌ MainActivity methods are NOT traceable")
        }
    }
}