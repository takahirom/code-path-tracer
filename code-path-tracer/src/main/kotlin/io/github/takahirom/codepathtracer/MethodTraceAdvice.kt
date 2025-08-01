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
 * Entry representing depth information for a single method enter
 */
data class DepthStackEntry(
    val baseDepth: Int,              // The depth before adding context methods
    val contextMethodsCount: Int,    // Number of context methods added for this enter
    val totalDepth: Int              // The final depth used for display (baseDepth + contextMethodsCount)
)

/**
 * Stack-based depth manager to properly handle enter/exit pairs and prevent accumulation
 */
data class DepthManager(
    private val depthStack: MutableList<DepthStackEntry> = mutableListOf()
) {
    /**
     * Called when entering a method. Records the depth information.
     * Returns the depth to use for display of the actual method.
     */
    fun onMethodEnter(contextMethodsCount: Int): Int {
        val baseDepth = if (depthStack.isEmpty()) 0 else depthStack.last().totalDepth
        val actualMethodDepth = baseDepth + contextMethodsCount
        val totalDepth = actualMethodDepth + 1  // +1 for the actual method itself
        
        val entry = DepthStackEntry(baseDepth, contextMethodsCount, totalDepth)
        depthStack.add(entry)
        
        return actualMethodDepth  // Return depth for actual method display
    }
    
    /**
     * Called when exiting a method. Removes the last entry from stack.
     * Returns the depth to use for display.
     */
    fun onMethodExit(): Int {
        if (depthStack.isEmpty()) return 0
        
        val entry = depthStack.removeLastOrNull() ?: return 0
        // Return display depth (matches enter depth)
        return entry.totalDepth - 1
    }
    
    /**
     * Get current depth without modifying the stack
     */
    fun getCurrentDepth(): Int {
        return depthStack.lastOrNull()?.totalDepth ?: 0
    }
    
    /**
     * Clear the stack (for cleanup)
     */
    fun clear() {
        depthStack.clear()
    }
}

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
        
        private val actualDepthCounter = ThreadLocal.withInitial { 0 }
        private val depthManager = ThreadLocal<DepthManager>()
        private val isTracing = ThreadLocal.withInitial { false }
        private val callStack = ThreadLocal<MutableList<CallContext>>()
        private val contextExitTracker = ThreadLocal<ContextExitTracker>()
        
        private fun getCallPath(config: CodePathTracer.Config): List<CallContext> {
            return if (config.beforeContextSize > 0) callStack.get() ?: emptyList() else emptyList()
        }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(
            // @net.bytebuddy.asm.Advice.Origin method: String,
            @net.bytebuddy.asm.Advice.Origin clazz: Class<*>,
            @net.bytebuddy.asm.Advice.Origin("#m") methodName: String,
            @net.bytebuddy.asm.Advice.Origin("#d") descriptor: String,
            @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>
        ) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            // Track actual depth for call stack (ALL methods, not just filtered)
            val depth = actualDepthCounter.get() ?: 0
            actualDepthCounter.set(depth + 1)
            
            val currentCallPath = getCallPath(config)
            
            // Create AdviceData with reliable ByteBuddy @Origin information
            val adviceData = AdviceData.Enter(
                args = args,
                depth = depth,
                clazz = clazz,           // Reliable class information
                methodName = methodName,  // Reliable method name (<init> for constructors)
                descriptor = descriptor   // Method descriptor
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
                
                /*
                 * Available ByteBuddy @Origin information:
                 * 
                 * method (String): 
                 *   - Normal method: "public final void io.github.takahirom.codepathtracersample.ConstructorTest.testConstructorMethodName()"
                 *   - Constructor: "public io.github.takahirom.codepathtracersample.TestClass(java.lang.String)"
                 * 
                 * executable (java.lang.reflect.Executable):
                 *   - executable.declaringClass.name: Full class name "io.github.takahirom.codepathtracersample.TestClass"
                 *   - executable.name: 
                 *     * Normal method: Method name "testConstructorMethodName"
                 *     * Constructor: Full class name "io.github.takahirom.codepathtracersample.TestClass" (problematic!)
                 *   - executable is Constructor<*>: true/false for constructor detection
                 *   - constructor.parameterTypes: [class java.lang.String] (if constructor)
                 * 
                 * clazz (Class<*>):
                 *   - clazz.name: Full class name "io.github.takahirom.codepathtracersample.TestClass"
                 *   - clazz.simpleName: Simple class name "TestClass"
                 * 
                 * Custom patterns (@Origin("#pattern")):
                 *   - #t: declaring type (class name)
                 *   - #m: method name ("<init>" for constructors, "<clinit>" for static initializers) 
                 *   - #d: method descriptor "(Ljava/lang/String;)V"
                 *   - #s: method signature (if available)
                 *   - #r: return type
                 *   - #l: line number (rarely available, needs debug info)
                 *   - #p: property name
                 * 
                 * Recommended approach:
                 *   - className: clazz.name (full class name)
                 *   - methodName: executable.name (for normal methods) or "<init>" (for constructors)
                 *   - isConstructor: executable is Constructor<*>
                 *   - lineNumber: lineNumber.toIntOrNull() (if debug info available)
                 */
                
                
                // Generate context Enter events if enabled
                if (config.beforeContextSize > 0) {
                    val stack = callStack.get()
                    if (stack != null && stack.size > 1) {
                        val contextMethods = stack.takeLast(minOf(config.beforeContextSize + 1, stack.size))
                            .dropLast(1) // Remove current method
                        
                        // Initialize depth manager and context exit tracker if needed
                        val depthMgr = depthManager.get() ?: DepthManager().also { depthManager.set(it) }
                        val tracker = contextExitTracker.get() ?: ContextExitTracker().also { contextExitTracker.set(it) }
                        
                        // Calculate depths using the new depth manager
                        val baseDepth = depthMgr.getCurrentDepth()
                        val methodsToShow = tracker.queueContextExits(contextMethods, baseDepth)
                        
                        // Generate Enter events only for methods that should be shown
                        methodsToShow.forEachIndexed { i, contextMethod ->
                            val contextEnterEvent = TraceEvent.Enter(
                                className = contextMethod.className,
                                methodName = contextMethod.methodName,
                                args = arrayOf(),
                                depth = baseDepth + i,
                                callPath = currentCallPath
                            )
                            println(config.formatter(contextEnterEvent))
                        }

                        // Record the depth information in the depth manager
                        // Note: We need to account for ALL context methods, not just the ones shown
                        val allContextMethodsCount = contextMethods.size
                        val actualMethodDepth = depthMgr.onMethodEnter(allContextMethodsCount)
                        
                        
                        // Update the display depth for the actual method entry
                        val adjustedEvent = when (traceEvent) {
                            is TraceEvent.Enter -> traceEvent.copy(depth = actualMethodDepth)
                            is TraceEvent.Exit -> traceEvent.copy(depth = actualMethodDepth)
                        }
                        println(config.formatter(adjustedEvent))
                        return  // Exit early since we've already printed
                    } else {
                        // No context methods, use depth manager for simple case
                        val depthMgr = depthManager.get() ?: DepthManager().also { depthManager.set(it) }
                        val actualMethodDepth = depthMgr.onMethodEnter(0)
                        
                        // Format with managed depth and print
                        val adjustedEvent = when (traceEvent) {
                            is TraceEvent.Enter -> traceEvent.copy(depth = actualMethodDepth)
                            is TraceEvent.Exit -> traceEvent.copy(depth = actualMethodDepth)
                        }
                        println(config.formatter(adjustedEvent))
                        return  // Exit early since we've already printed
                    }
                }
                
                // Initialize depth manager if needed (for non-context case)
                val depthMgr = depthManager.get() ?: DepthManager().also { depthManager.set(it) }
                
                // For filtered methods without context
                val displayDepth = depthMgr.onMethodEnter(0)
                
                // Format with managed depth and print
                val adjustedEvent = when (traceEvent) {
                    is TraceEvent.Enter -> traceEvent.copy(depth = displayDepth)
                    is TraceEvent.Exit -> traceEvent.copy(depth = displayDepth)
                }
                println(config.formatter(adjustedEvent))
            } finally {
                isTracing.set(false)
            }
        }
        
        @JvmStatic  
        @net.bytebuddy.asm.Advice.OnMethodExit
        fun methodExit(
            // @net.bytebuddy.asm.Advice.Origin method: String,
            @net.bytebuddy.asm.Advice.Origin clazz: Class<*>,
            @net.bytebuddy.asm.Advice.Origin("#m") methodName: String,
            @net.bytebuddy.asm.Advice.Origin("#d") descriptor: String,
            @net.bytebuddy.asm.Advice.Return(typing = net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC) returnValue: Any?
        ) {
            // Prevent infinite recursion
            if (isTracing.get()) return
            
            val config = CodePathTracerAgent.getConfig() ?: return
            
            // Track actual depth for call stack (ALL methods, not just filtered)
            val depth = (actualDepthCounter.get() ?: 1) - 1
            actualDepthCounter.set(depth)
            
            val currentCallPath = getCallPath(config)
            
            // Create AdviceData with reliable ByteBuddy @Origin information
            val adviceData = AdviceData.Exit(
                returnValue = returnValue,
                depth = depth,
                clazz = clazz,           // Reliable class information
                methodName = methodName,  // Reliable method name (<init> for constructors)
                descriptor = descriptor   // Method descriptor
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
                    // Use depth manager for proper depth management
                    val depthMgr = depthManager.get() ?: DepthManager().also { depthManager.set(it) }
                    val exitDepth = depthMgr.onMethodExit()
                    val adjustedEvent = (traceEvent as TraceEvent.Exit).copy(depth = maxOf(0, exitDepth))
                    println(config.formatter(adjustedEvent))
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
            actualDepthCounter.remove()
            depthManager.get()?.clear()
            depthManager.remove()
            isTracing.remove()
            callStack.remove()
            contextExitTracker.remove()
        }
    }
}