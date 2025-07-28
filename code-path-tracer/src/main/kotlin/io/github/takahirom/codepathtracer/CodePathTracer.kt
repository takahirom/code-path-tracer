package io.github.takahirom.codepathtracer

/**
 * Code Path Tracer - Main API for tracing method execution paths
 * 
 * Usage:
 * ```
 * codePathTrace(config) {
 *     // Code to trace
 *     MainActivity().onCreate(null)
 * }
 * 
 * // Or with custom configuration
 * val config = CodePathTracer.Config(
 *     filter = { event -> event.className.startsWith("com.example") },
 *     formatter = { event -> "${event.className}.${event.methodName}" }
 * )
 * codePathTrace(config) {
 *     // Traced code here
 * }
 * ```
 */
class CodePathTracer(private val config: Config) {
    
    
    data class Config(
        val filter: (TraceEvent) -> Boolean = DefaultFilter::filter,
        val formatter: (TraceEvent) -> String = DefaultFormatter::format,
        val enabled: Boolean = true,
        val autoRetransform: Boolean = true,
        val traceEventGenerator: (AdviceData) -> TraceEvent? = { advice -> defaultTraceEventGenerator(advice) },
        val maxToStringLength: Int = 30
    )
    
    /**
     * Execute code with tracing enabled
     */
    fun <T> trace(block: () -> T): T {
        if (!config.enabled) {
            return block()
        }
        
        // Initialize agent with config
        CodePathTracerAgent.initialize(config)
        
        return try {
            block()
        } finally {
            // Reset tracing to stop after this block
            CodePathTracerAgent.reset()
        }
    }
    
    companion object {
        /**
         * Global debug flag for all tracing components
         */
        var DEBUG = false
        
        /**
         * Default implementation to convert AdviceData to TraceEvent
         */
        fun defaultTraceEventGenerator(advice: AdviceData): TraceEvent? {
            return when (advice) {
                is AdviceData.Enter -> {
                    val methodInfo = parseMethodInfo(advice.method)
                    TraceEvent.Enter(
                        className = methodInfo.className,
                        methodName = methodInfo.methodName,
                        args = advice.args,
                        depth = advice.depth
                    )
                }
                is AdviceData.Exit -> {
                    val methodInfo = parseMethodInfo(advice.method)
                    TraceEvent.Exit(
                        className = methodInfo.className,
                        methodName = methodInfo.methodName,
                        returnValue = advice.returnValue,
                        depth = advice.depth
                    )
                }
            }
        }
        
        private data class MethodInfo(val className: String, val methodName: String)
        
        private fun parseMethodInfo(method: String): MethodInfo {
            val parenIndex = method.indexOf('(')
            val methodPart = if (parenIndex >= 0) method.substring(0, parenIndex) else method
            
            val spaceIndex = methodPart.lastIndexOf(' ')
            val cleanMethodPart = if (spaceIndex >= 0) methodPart.substring(spaceIndex + 1) else methodPart
            
            val lastDotIndex = cleanMethodPart.lastIndexOf('.')
            val className = if (lastDotIndex >= 0) cleanMethodPart.substring(0, lastDotIndex) else "Unknown"
            val methodName = if (lastDotIndex >= 0) cleanMethodPart.substring(lastDotIndex + 1) else cleanMethodPart
            
            return MethodInfo(className, methodName)
        }
        
        /**
         * Create a builder for configuration
         */
        fun builder() = Builder()
        
        /**
         * Simple configuration with default settings
         */
        fun simple() = Config()
    }
    
    class Builder {
        private var filter: (TraceEvent) -> Boolean = { true }
        private var formatter: (TraceEvent) -> String = DefaultFormatter::format
        private var enabled = true
        
        fun filter(predicate: (TraceEvent) -> Boolean) = apply { filter = predicate }
        fun formatter(format: (TraceEvent) -> String) = apply { formatter = format }
        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        
        fun build(): Config = Config(filter, formatter, enabled)
    }
}

/**
 * DSL function for tracing code execution
 */
fun <T> codePathTrace(config: CodePathTracer.Config = CodePathTracer.simple(), block: () -> T): T {
    return CodePathTracer(config).trace(block)
}

/**
 * DSL function with builder configuration
 */
inline fun <T> codePathTrace(configBuilder: CodePathTracer.Builder.() -> Unit, noinline block: () -> T): T {
    val config = CodePathTracer.builder().apply(configBuilder).build()
    return codePathTrace(config, block)
}