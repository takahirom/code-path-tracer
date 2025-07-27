package io.github.takahirom.codepathfinder

/**
 * Static class for ByteBuddy Advice
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
            // Avoid Kotlin string operations that might cause recursion
            val parenIndex = method.indexOf('(')
            val methodPart = if (parenIndex >= 0) method.substring(0, parenIndex) else method
            
            // Simple parsing without complex string operations
            val spaceIndex = methodPart.lastIndexOf(' ')
            val cleanMethodPart = if (spaceIndex >= 0) methodPart.substring(spaceIndex + 1) else methodPart
            
            
            val lastDotIndex = cleanMethodPart.lastIndexOf('.')
            val className = if (lastDotIndex >= 0) cleanMethodPart.substring(0, lastDotIndex) else "Unknown"
            val methodName = if (lastDotIndex >= 0) cleanMethodPart.substring(lastDotIndex + 1) else cleanMethodPart
            
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
        fun methodExit(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.Return(typing = net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC) returnValue: Any?) {
            val config = MethodTraceAgent.getConfig() ?: return
            
            val depth = (depthCounter.get() ?: 1) - 1
            depthCounter.set(depth)
            
            // Parse method signature to extract class and method name
            // Avoid Kotlin string operations that might cause recursion
            val parenIndex = method.indexOf('(')
            val methodPart = if (parenIndex >= 0) method.substring(0, parenIndex) else method
            
            // Simple parsing without complex string operations
            val spaceIndex = methodPart.lastIndexOf(' ')
            val cleanMethodPart = if (spaceIndex >= 0) methodPart.substring(spaceIndex + 1) else methodPart
            
            val lastDotIndex = cleanMethodPart.lastIndexOf('.')
            val className = if (lastDotIndex >= 0) cleanMethodPart.substring(0, lastDotIndex) else "Unknown"
            val methodName = if (lastDotIndex >= 0) cleanMethodPart.substring(lastDotIndex + 1) else cleanMethodPart
            
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