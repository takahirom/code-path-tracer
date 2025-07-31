package io.github.takahirom.codepathtracer

/**
 * Data class to represent a method call in the call stack
 */
data class CallContext(
    val className: String,
    val methodName: String,
    val depth: Int
)

/**
 * Information about a context exit event that needs to be shown
 */
data class ContextExitInfo(
    val className: String,
    val methodName: String,
    val actualDepth: Int,        // The actual depth of the context method for triggering exit
    val displayDepth: Int,       // The filtered depth used for display
    val wasGenerated: Boolean = true // Marks this as a context-generated exit
)

/**
 * Key to uniquely identify a context method for deduplication
 */
data class ContextMethodKey(
    val className: String,
    val methodName: String,
    val actualDepth: Int // The original depth where this method was called
)

/**
 * Stack-like structure to track pending context exit events and prevent duplicates
 */
data class ContextExitTracker(
    val pendingExits: MutableList<ContextExitInfo> = mutableListOf(),
    val shownContextEnters: MutableSet<ContextMethodKey> = mutableSetOf()
) {
    /**
     * Queue context exits for methods that were shown as context enters.
     * Returns list of context methods that should actually be shown (excluding duplicates).
     * These should be added in reverse order (deepest first) so they can be popped correctly
     */
    fun queueContextExits(contextMethods: List<CallContext>, startingDepth: Int): List<CallContext> {
        val methodsToShow = mutableListOf<CallContext>()
        
        // Check each context method for duplicates
        for (i in contextMethods.indices) {
            val contextMethod = contextMethods[i]
            val methodKey = ContextMethodKey(
                className = contextMethod.className,
                methodName = contextMethod.methodName,
                actualDepth = contextMethod.depth
            )
            
            // Only show if we haven't shown this context enter before
            if (!shownContextEnters.contains(methodKey)) {
                shownContextEnters.add(methodKey)
                methodsToShow.add(contextMethod)
                
                // Queue corresponding exit - use actual depth of context method for comparison
                val exitInfo = ContextExitInfo(
                    className = contextMethod.className,
                    methodName = contextMethod.methodName,
                    actualDepth = contextMethod.depth, // Use actual depth for triggering exit
                    displayDepth = startingDepth + i   // Use filtered depth for display
                )
                pendingExits.add(exitInfo)
            }
        }
        
        return methodsToShow
    }
    
    /**
     * Get and remove context exits that should be shown when a method at the given depth exits
     * Returns exits in correct order (shallowest depth first)
     */
    fun popContextExitsForDepth(actualDepth: Int): List<ContextExitInfo> {
        val exitsToShow = mutableListOf<ContextExitInfo>()
        
        // Find all exits that should be shown when this depth exits
        // We want to show context exits from deepest to shallowest
        while (pendingExits.isNotEmpty()) {
            val nextExit = pendingExits.last()
            // Check if this context exit should be shown
            // Show it only when the exact context method exits (depth must match exactly)
            if (actualDepth == nextExit.actualDepth) {
                val exitToShow = pendingExits.removeLastOrNull() ?: break
                exitsToShow.add(0, exitToShow) // Add to front for correct order
                
                // Remove from shown context enters when we show its exit
                val methodKey = ContextMethodKey(
                    className = exitToShow.className,
                    methodName = exitToShow.methodName,
                    actualDepth = exitToShow.actualDepth // Use context method's original depth for cleanup
                )
                shownContextEnters.remove(methodKey)
            } else {
                break
            }
        }
        
        return exitsToShow
    }
}

/**
 * ByteBuddy advice for method tracing
 */
class MethodTraceAdvice {
    companion object {
        
        private val depthCounter = ThreadLocal.withInitial { 0 }
        private val filteredDepthCounter = ThreadLocal.withInitial { 0 }
        private val isTracing = ThreadLocal.withInitial { false }
        private val callStack = ThreadLocal<MutableList<CallContext>>()
        private val contextExitTracker = ThreadLocal<ContextExitTracker>()
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            
            // Get current call stack for callPath
            val currentCallPath = if (config.beforeContextSize > 0) {
                callStack.get() ?: mutableListOf()
            } else {
                mutableListOf()
            }
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Enter(
                method = method,
                args = args,
                depth = depth
            )
            var traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            // Add callPath to traceEvent
            traceEvent = when (traceEvent) {
                is TraceEvent.Enter -> traceEvent.copy(callPath = currentCallPath.toList())
                is TraceEvent.Exit -> traceEvent.copy(callPath = currentCallPath.toList())
            }
            
            // Initialize call stack if beforeContextSize > 0
            if (config.beforeContextSize > 0 && callStack.get() == null) {
                callStack.set(mutableListOf())
            }
            
            try {
                isTracing.set(true)
                
                // Add to call stack if enabled
                if (config.beforeContextSize > 0) {
                    val stack = callStack.get()
                    if (stack != null) {
                        stack.add(CallContext(
                            className = traceEvent.className,
                            methodName = traceEvent.methodName,
                            depth = depth
                        ))
                    }
                }
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    depthCounter.set(depth + 1)  // Update depth but don't log
                    return
                }
                
                // Generate context Enter events if enabled
                if (config.beforeContextSize > 0) {
                    val stack = callStack.get()
                    if (stack != null && stack.size > 1) {
                        val contextMethods = stack.takeLast(minOf(config.beforeContextSize + 1, stack.size))
                            .dropLast(1) // Remove current method
                        
                        // Initialize context exit tracker if needed
                        if (contextExitTracker.get() == null) {
                            contextExitTracker.set(ContextExitTracker())
                        }
                        
                        // Get context methods to show (excluding duplicates) and queue their exits
                        val startingDepth = maxOf(0, filteredDepthCounter.get() - contextMethods.size)
                        val methodsToShow = contextExitTracker.get()?.queueContextExits(contextMethods, startingDepth) ?: emptyList()
                        
                        // Generate Enter events only for methods that should be shown
                        var contextDepth = startingDepth
                        for (contextMethod in methodsToShow) {
                            val contextEnterEvent = TraceEvent.Enter(
                                className = contextMethod.className,
                                methodName = contextMethod.methodName,
                                args = arrayOf(), // Context methods don't have arg info
                                depth = contextDepth,
                                callPath = currentCallPath.toList()
                            )
                            val formattedContext = config.formatter(contextEnterEvent)
                            println(formattedContext)
                            contextDepth++
                        }
                        
                        // Update filtered depth counter to account for shown context methods
                        filteredDepthCounter.set(filteredDepthCounter.get() + methodsToShow.size)
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
            
            // Get current call stack for callPath
            val currentCallPath = if (config.beforeContextSize > 0) {
                callStack.get() ?: mutableListOf()
            } else {
                mutableListOf()
            }
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Exit(
                method = method,
                returnValue = returnValue,
                depth = depth
            )
            var traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            // Add callPath to traceEvent
            traceEvent = when (traceEvent) {
                is TraceEvent.Enter -> traceEvent.copy(callPath = currentCallPath.toList())
                is TraceEvent.Exit -> traceEvent.copy(callPath = currentCallPath.toList())
            }
            
            
            try {
                isTracing.set(true)
                
                // Remove from call stack if enabled
                if (config.beforeContextSize > 0) {
                    val stack = callStack.get()
                    if (stack != null && stack.isNotEmpty()) {
                        stack.removeLastOrNull()
                    }
                }
                
                // Check if this method passes the filter
                val passesFilter = config.filter(traceEvent)
                
                if (passesFilter) {
                    // Format with current filtered depth before decrementing
                    val currentFilteredDepth = filteredDepthCounter.get() ?: 0
                    val adjustedEvent = when (traceEvent) {
                        is TraceEvent.Exit -> traceEvent.copy(depth = maxOf(0, currentFilteredDepth - 1))
                        else -> traceEvent // Should not happen in methodExit
                    }
                    val formattedOutput = config.formatter(adjustedEvent)
                    println(formattedOutput)
                    
                    // Update filtered depth counter after formatting
                    val newFilteredDepth = maxOf(0, currentFilteredDepth - 1)
                    filteredDepthCounter.set(newFilteredDepth)
                }
                
                // Generate context Exit events if enabled and we have queued exits
                // This runs for ALL method exits (filtered or not) to catch context method exits
                if (config.beforeContextSize > 0) {
                    val exitTracker = contextExitTracker.get()
                    if (exitTracker != null) {
                        // Get context exits that should be shown when this method exits
                        val contextExitsToShow = exitTracker.popContextExitsForDepth(depth)
                        
                        // Show context exit events in correct order (shallowest depth first)
                        for (contextExit in contextExitsToShow) {
                            val contextExitEvent = TraceEvent.Exit(
                                className = contextExit.className,
                                methodName = contextExit.methodName,
                                returnValue = null, // Context methods don't have return value info
                                depth = contextExit.displayDepth, // Use the stored display depth
                                callPath = currentCallPath.toList()
                            )
                            val formattedContextExit = config.formatter(contextExitEvent)
                            println(formattedContextExit)
                        }
                    }
                }
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
            callStack.remove()
            contextExitTracker.remove()
        }
    }
}