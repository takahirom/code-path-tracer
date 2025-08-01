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
    val displayDepth: Int        // The filtered depth used for display
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
        return contextMethods.mapIndexedNotNull { i, method ->
            val methodKey = ContextMethodKey(method.className, method.methodName, method.depth)
            if (shownContextEnters.add(methodKey)) {
                pendingExits.add(ContextExitInfo(method.className, method.methodName, method.depth, startingDepth + i))
                method
            } else null
        }
    }
    
    /**
     * Get and remove context exits that should be shown when a method at the given depth exits
     * Returns exits in correct order (shallowest depth first)
     */
    fun popContextExitsForDepth(actualDepth: Int): List<ContextExitInfo> {
        val exitsToShow = mutableListOf<ContextExitInfo>()
        while (pendingExits.isNotEmpty() && pendingExits.last().actualDepth == actualDepth) {
            val exit = pendingExits.removeLastOrNull() ?: break
            exitsToShow.add(0, exit)
            shownContextEnters.remove(ContextMethodKey(exit.className, exit.methodName, exit.actualDepth))
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
        
        private fun getCallPath(config: CodePathTracer.Config): List<CallContext> {
            return if (config.beforeContextSize > 0) callStack.get() ?: emptyList() else emptyList()
        }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            
            // Always increment depth for ALL methods, regardless of filter
            depthCounter.set(depth + 1)
            
            val currentCallPath = getCallPath(config)
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Enter(
                method = method,
                args = args,
                depth = depth
            )
            var traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            // Add callPath to traceEvent
            traceEvent = when (traceEvent) {
                is TraceEvent.Enter -> traceEvent.copy(callPath = currentCallPath)
                is TraceEvent.Exit -> traceEvent.copy(callPath = currentCallPath)
            }
            
            // Initialize call stack if beforeContextSize > 0
            if (config.beforeContextSize > 0 && callStack.get() == null) {
                callStack.set(mutableListOf())
            }
            
            try {
                isTracing.set(true)
                
                // Add to call stack if enabled
                if (config.beforeContextSize > 0) {
                    callStack.get()?.add(CallContext(traceEvent.className, traceEvent.methodName, depth))
                }
                
                // Apply filter
                if (!config.filter(traceEvent)) {
                    return  // Don't log filtered methods, but depth was already incremented
                }
                
                // Generate context Enter events if enabled
                if (config.beforeContextSize > 0) {
                    val stack = callStack.get()
                    if (stack != null && stack.size > 1) {
                        val contextMethods = stack.takeLast(minOf(config.beforeContextSize + 1, stack.size))
                            .dropLast(1) // Remove current method
                        
                        // Initialize context exit tracker if needed and get methods to show
                        val tracker = contextExitTracker.get() ?: ContextExitTracker().also { contextExitTracker.set(it) }
                        val startingDepth = maxOf(0, filteredDepthCounter.get() - contextMethods.size)
                        val methodsToShow = tracker.queueContextExits(contextMethods, startingDepth)
                        
                        // Generate Enter events only for methods that should be shown
                        methodsToShow.forEachIndexed { i, contextMethod ->
                            val contextEnterEvent = TraceEvent.Enter(
                                className = contextMethod.className,
                                methodName = contextMethod.methodName,
                                args = arrayOf(),
                                depth = startingDepth + i,
                                callPath = currentCallPath
                            )
                            println(config.formatter(contextEnterEvent))
                        }
                        
                        // Update filtered depth counter to account for shown context methods
                        filteredDepthCounter.set(filteredDepthCounter.get() + methodsToShow.size)
                    }
                }
                
                // Format with filtered depth and print
                val adjustedEvent = when (traceEvent) {
                    is TraceEvent.Enter -> traceEvent.copy(depth = filteredDepthCounter.get())
                    is TraceEvent.Exit -> traceEvent.copy(depth = filteredDepthCounter.get())
                }
                println(config.formatter(adjustedEvent))
                
                filteredDepthCounter.set(filteredDepthCounter.get() + 1)
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
            
            val currentCallPath = getCallPath(config)
            
            // Create AdviceData and convert to TraceEvent using traceEventGenerator
            val adviceData = AdviceData.Exit(
                method = method,
                returnValue = returnValue,
                depth = depth
            )
            var traceEvent = config.traceEventGenerator(adviceData) ?: return
            
            // Add callPath to traceEvent
            traceEvent = when (traceEvent) {
                is TraceEvent.Enter -> traceEvent.copy(callPath = currentCallPath)
                is TraceEvent.Exit -> traceEvent.copy(callPath = currentCallPath)
            }
            
            
            try {
                isTracing.set(true)
                
                // Remove from call stack if enabled
                if (config.beforeContextSize > 0) {
                    callStack.get()?.takeIf { it.isNotEmpty() }?.removeLastOrNull()
                }
                
                // Check if this method passes the filter
                val passesFilter = config.filter(traceEvent)
                
                if (passesFilter) {
                    val currentDepth = filteredDepthCounter.get() ?: 0
                    val adjustedEvent = (traceEvent as TraceEvent.Exit).copy(depth = maxOf(0, currentDepth - 1))
                    println(config.formatter(adjustedEvent))
                    filteredDepthCounter.set(maxOf(0, currentDepth - 1))
                }
                
                // Generate context Exit events if enabled and we have queued exits  
                // This runs for ALL method exits (filtered or not) to catch context method exits
                if (config.beforeContextSize > 0) {
                    contextExitTracker.get()?.popContextExitsForDepth(depth)?.forEach { exit ->
                        println(config.formatter(
                            TraceEvent.Exit(exit.className, exit.methodName, null, exit.displayDepth, currentCallPath)
                        ))
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