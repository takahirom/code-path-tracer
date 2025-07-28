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
        val enabled: Boolean = true
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
            // Any cleanup if needed
        }
    }
    
    companion object {
        /**
         * Global debug flag for all tracing components
         */
        const val DEBUG = false
        
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
        private var formatter: (TraceEvent) -> String = TraceEvent::defaultFormat
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