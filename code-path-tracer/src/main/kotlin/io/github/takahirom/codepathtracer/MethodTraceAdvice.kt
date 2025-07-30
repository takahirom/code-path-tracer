package io.github.takahirom.codepathtracer

/**
 * ByteBuddy advice for method tracing
 */
class MethodTraceAdvice {
    companion object {
        
        private val depthCounter = ThreadLocal.withInitial { 0 }
        private val isTracing = ThreadLocal.withInitial { false }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Enter(
                method = method,
                args = args,
                depth = depth
            )
            val traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            try {
                isTracing.set(true)
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    depthCounter.set(depth + 1)  // Update depth but don't log
                    return
                }
                
                // Format and print (formatter handles enter/exit formatting)
                val formattedOutput = config.formatter(traceEvent)
                println(formattedOutput)
                depthCounter.set(depth + 1)
            } finally {
                isTracing.set(false)
            }
        }
        
        @JvmStatic  
        @net.bytebuddy.asm.Advice.OnMethodExit
        fun methodExit(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.Return(typing = net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC) returnValue: Any?) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = (depthCounter.get() ?: 1) - 1
            depthCounter.set(depth)
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Exit(
                method = method,
                returnValue = returnValue,
                depth = depth
            )
            val traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            try {
                isTracing.set(true)
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    return
                }
                
                // Format and print (formatter handles enter/exit formatting)
                val formattedOutput = config.formatter(traceEvent)
                println(formattedOutput)
            } finally {
                isTracing.set(false)
            }
        }
        
        /**
         * Clean up ThreadLocal variables to prevent memory leaks
         */
        @JvmStatic
        fun cleanup() {
            depthCounter.remove()
            isTracing.remove()
        }
        
        /**
         * Force cleanup all ThreadLocal variables across all threads
         * Useful for test environments where coroutines might leave traces
         */
        @JvmStatic
        fun forceCleanupAllThreads() {
            cleanup()
            // Force GC to clean up any remaining ThreadLocal references
            System.gc()
        }
    }
}