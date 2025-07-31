package io.github.takahirom.codepathtracer

/**
 * ByteBuddy advice for method tracing
 */
class MethodTraceAdvice {
    companion object {
        
        private val depthCounter = ThreadLocal.withInitial { 0 }
        private val filteredDepthCounter = ThreadLocal.withInitial { 0 }
        private val isTracing = ThreadLocal.withInitial { false }
        private val eventBuffer = ThreadLocal<CircularBuffer<TraceEvent>>()
        
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
            
            // Initialize buffer if beforeContextSize > 0
            if (config.beforeContextSize > 0 && eventBuffer.get() == null) {
                eventBuffer.set(CircularBuffer(config.beforeContextSize * 2))
            }
            
            try {
                isTracing.set(true)
                
                // Store event in buffer before filtering (if buffer enabled)
                eventBuffer.get()?.add(traceEvent)
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    depthCounter.set(depth + 1)  // Update depth but don't log
                    return
                }
                
                // Print buffered context events if enabled
                if (config.beforeContextSize > 0) {
                    val buffer = eventBuffer.get()
                    if (buffer != null) {
                        val contextEvents = buffer.getLast(config.beforeContextSize)
                        for (contextEvent in contextEvents) {
                            if (!config.filter(contextEvent)) {
                                val contextFormatted = config.formatter(contextEvent)
                                println("  [context] $contextFormatted")
                            }
                        }
                    }
                }
                
                // Format with filtered depth and print
                val filteredDepth = filteredDepthCounter.get()
                val adjustedEvent = when (traceEvent) {
                    is TraceEvent.Enter -> traceEvent.copy(depth = filteredDepth)
                    is TraceEvent.Exit -> traceEvent.copy(depth = filteredDepth)
                }
                val formattedOutput = config.formatter(adjustedEvent)
                println(formattedOutput)
                
                depthCounter.set(depth + 1)
                filteredDepthCounter.set(filteredDepth + 1)
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
            
            // Initialize buffer if beforeContextSize > 0
            if (config.beforeContextSize > 0 && eventBuffer.get() == null) {
                eventBuffer.set(CircularBuffer(config.beforeContextSize * 2))
            }
            
            try {
                isTracing.set(true)
                
                // Store event in buffer before filtering (if buffer enabled)
                eventBuffer.get()?.add(traceEvent)
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    return
                }
                
                // Format with filtered depth and print
                val filteredDepth = (filteredDepthCounter.get() ?: 1) - 1
                filteredDepthCounter.set(maxOf(0, filteredDepth))
                
                val adjustedEvent = when (traceEvent) {
                    is TraceEvent.Enter -> traceEvent.copy(depth = filteredDepth)
                    is TraceEvent.Exit -> traceEvent.copy(depth = filteredDepth)
                }
                val formattedOutput = config.formatter(adjustedEvent)
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
            filteredDepthCounter.remove()
            isTracing.remove()
            eventBuffer.remove()
        }
    }
}