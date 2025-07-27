package io.github.takahirom.codepathfinder

/**
 * Static class for ByteBuddy Advice (TraceEvent version)
 */
class MethodTraceAdvice {
    companion object {
        private val depthCounter = ThreadLocal.withInitial { 0 }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            
            val config = MethodTraceAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            
            // Parse method signature to extract class and method name
            val methodPart = method.substringBefore("(")
                .replace("public static final ", "")
                .replace("public final ", "")
                .replace("private final ", "")
                .replace("static final ", "")
                .replace("public ", "")
                .replace("private ", "")
                .replace("static ", "")
            
            // Handle return type - method format is "returnType className.methodName"
            val parts = methodPart.split(" ")
            val classMethodPart = if (parts.size > 1) {
                parts.drop(1).joinToString(" ") // Skip return type
            } else {
                methodPart
            }
            
            val dotParts = classMethodPart.split(".")
            val className = if (dotParts.size >= 2) {
                dotParts.dropLast(1).joinToString(".")
            } else {
                "Unknown"
            }
            val methodName = dotParts.lastOrNull() ?: "unknown"
            
            // Create TraceEvent for filtering
            val traceEvent = TraceEvent(
                className = className,
                methodName = methodName,
                args = args,
                depth = depth
            )
            
            // Apply filter
            if (!config.filter(traceEvent)) {
                depthCounter.set(depth + 1)  // Update depth but don't log
                return
            }
            
            // Format and print
            val formattedOutput = config.formatter(traceEvent)
            println("$formattedOutput")
            depthCounter.set(depth + 1)
        }
        
        @JvmStatic  
        @net.bytebuddy.asm.Advice.OnMethodExit
        fun methodExit(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.Return returnValue: Any?) {
            val config = MethodTraceAgent.getConfig() ?: return
            
            val depth = (depthCounter.get() ?: 1) - 1
            depthCounter.set(depth)
            
            // Parse method signature to extract class and method name
            val methodPart = method.substringBefore("(")
                .replace("public static final ", "")
                .replace("public final ", "")
                .replace("private final ", "")
                .replace("static final ", "")
                .replace("public ", "")
                .replace("private ", "")
                .replace("static ", "")
            
            val parts = methodPart.split(".")
            val className = if (parts.size >= 2) {
                parts.dropLast(1).joinToString(".")
            } else {
                "Unknown"
            }
            val methodName = parts.lastOrNull() ?: "unknown"
            
            // Create TraceEvent for filtering (exit event with return value)
            val traceEvent = TraceEvent(
                className = className,
                methodName = methodName,
                args = emptyArray(), // Args not available at exit
                returnValue = returnValue,
                depth = depth
            )
            
            // Apply filter
            if (!config.filter(traceEvent)) {
                return
            }
            
            // Format and print (could be different formatter for exit events)
            val formattedOutput = config.formatter(traceEvent)
            println("← $formattedOutput")
        }
    }
}