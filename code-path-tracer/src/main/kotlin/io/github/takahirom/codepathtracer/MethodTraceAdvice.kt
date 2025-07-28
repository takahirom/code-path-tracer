package io.github.takahirom.codepathtracer

/**
 * Static class for ByteBuddy Advice
 */
class MethodTraceAdvice {
    companion object {
        private val depthCounter = ThreadLocal.withInitial { 0 }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            
            val parenIndex = method.indexOf('(')
            val methodPart = if (parenIndex >= 0) method.substring(0, parenIndex) else method
            
            val spaceIndex = methodPart.lastIndexOf(' ')
            val cleanMethodPart = if (spaceIndex >= 0) methodPart.substring(spaceIndex + 1) else methodPart
            
            
            val lastDotIndex = cleanMethodPart.lastIndexOf('.')
            val className = if (lastDotIndex >= 0) cleanMethodPart.substring(0, lastDotIndex) else "Unknown"
            val methodName = if (lastDotIndex >= 0) cleanMethodPart.substring(lastDotIndex + 1) else cleanMethodPart
            
            // Create TraceEvent.Enter for filtering
            val traceEvent = TraceEvent.Enter(
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
            
            // Format and print (formatter handles enter/exit formatting)
            val formattedOutput = config.formatter(traceEvent)
            println(formattedOutput)
            depthCounter.set(depth + 1)
        }
        
        @JvmStatic  
        @net.bytebuddy.asm.Advice.OnMethodExit
        fun methodExit(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.Return(typing = net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC) returnValue: Any?) {
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = (depthCounter.get() ?: 1) - 1
            depthCounter.set(depth)
            
            val parenIndex = method.indexOf('(')
            val methodPart = if (parenIndex >= 0) method.substring(0, parenIndex) else method
            
            val spaceIndex = methodPart.lastIndexOf(' ')
            val cleanMethodPart = if (spaceIndex >= 0) methodPart.substring(spaceIndex + 1) else methodPart
            
            val lastDotIndex = cleanMethodPart.lastIndexOf('.')
            val className = if (lastDotIndex >= 0) cleanMethodPart.substring(0, lastDotIndex) else "Unknown"
            val methodName = if (lastDotIndex >= 0) cleanMethodPart.substring(lastDotIndex + 1) else cleanMethodPart
            
            // Create TraceEvent.Exit for filtering (exit event with return value)
            val traceEvent = TraceEvent.Exit(
                className = className,
                methodName = methodName,
                returnValue = returnValue,
                depth = depth
            )
            
            // Apply filter
            if (!config.filter(traceEvent)) {
                return
            }
            
            // Format and print (formatter handles enter/exit formatting)
            val formattedOutput = config.formatter(traceEvent)
            println(formattedOutput)
        }
    }
}