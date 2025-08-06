package io.github.takahirom.codepathtracer

/**
 * Code Path Tracer - Main API for tracing method execution paths
 * 
 * Usage:
 * ```
 * // Simple usage with default settings
 * codePathTrace {
 *     MainActivity().onCreate(null)
 * }
 * 
 * // Custom configuration with Builder
 * val tracer = CodePathTracer.Builder()
 *     .filter { event -> event.className.startsWith("com.example") }
 *     .formatter { event -> "${event.className}.${event.methodName}" }
 *     .build()
 * 
 * codePathTrace(tracer) {
 *     // Traced code here
 * }
 * ```
 */
class CodePathTracer private constructor(private val config: Config) {

    internal data class Config(
        val filter: (TraceEvent) -> Boolean = DefaultFilter::filter,
        val formatter: (TraceEvent) -> String = DefaultFormatter::format,
        val enabled: Boolean = true,
        val autoRetransform: Boolean = true,
        val traceEventGenerator: (AdviceData) -> TraceEvent? = { advice -> defaultTraceEventGenerator(advice) },
        val maxToStringLength: Int = 30,
        val beforeContextSize: Int = 0,
        val maxIndentDepth: Int = 60,
        val agentController: CodePathAgentController = CodePathAgentController.default()
    )
    
    /**
     * Execute code with tracing enabled
     */
    fun <T> trace(block: () -> T): T {
        return CodePathTracerCore.executeWithTracing(config, block)
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
                    TraceEvent.Enter(
                        className = advice.clazz.name,     // Reliable class name from ByteBuddy @Origin
                        methodName = advice.methodName,    // Reliable method name (<init> for constructors)
                        args = advice.args,
                        depth = advice.depth
                    )
                }
                is AdviceData.Exit -> {
                    TraceEvent.Exit(
                        className = advice.clazz.name,     // Reliable class name from ByteBuddy @Origin
                        methodName = advice.methodName,    // Reliable method name (<init> for constructors)
                        returnValue = advice.returnValue,
                        depth = advice.depth
                    )
                }
            }
        }
        
    }
    
    class Builder {
        private var filter: (TraceEvent) -> Boolean = DefaultFilter::filter
        private var formatter: (TraceEvent) -> String = DefaultFormatter::format
        private var enabled: Boolean = true
        private var autoRetransform: Boolean = true
        private var traceEventGenerator: (AdviceData) -> TraceEvent? = { advice -> defaultTraceEventGenerator(advice) }
        private var maxToStringLength: Int = 30
        private var beforeContextSize: Int = 0
        private var maxIndentDepth: Int = 60
        private var agentController: CodePathAgentController = CodePathAgentController.default()
        
        fun filter(predicate: (TraceEvent) -> Boolean) = apply { this.filter = predicate }
        fun formatter(format: (TraceEvent) -> String) = apply { this.formatter = format }
        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        fun autoRetransform(autoRetransform: Boolean) = apply { this.autoRetransform = autoRetransform }
        fun traceEventGenerator(generator: (AdviceData) -> TraceEvent?) = apply { this.traceEventGenerator = generator }
        fun maxToStringLength(length: Int) = apply { this.maxToStringLength = length }
        fun beforeContextSize(size: Int) = apply { this.beforeContextSize = size }
        fun maxIndentDepth(depth: Int) = apply { this.maxIndentDepth = depth }
        
        /**
         * Set custom agent controller for ByteBuddy configuration.
         * 
         * **IMPORTANT**: Agent configuration cannot be changed after first installation.
         * The first controller used will determine ByteBuddy settings for the entire
         * JVM process. Subsequent tracers with different controllers will reuse the
         * existing agent configuration.
         */
        fun codePathAgentController(controller: CodePathAgentController) = apply { this.agentController = controller }
        
        fun build(): CodePathTracer = CodePathTracer(Config(
            filter = filter,
            formatter = formatter,
            enabled = enabled,
            autoRetransform = autoRetransform,
            traceEventGenerator = traceEventGenerator,
            maxToStringLength = maxToStringLength,
            beforeContextSize = beforeContextSize,
            maxIndentDepth = maxIndentDepth,
            agentController = agentController
        ))
        
        fun asJUnitRule(): CodePathTracerRule = CodePathTracerRule(Config(
            filter = filter,
            formatter = formatter,
            enabled = enabled,
            autoRetransform = autoRetransform,
            traceEventGenerator = traceEventGenerator,
            maxToStringLength = maxToStringLength,
            beforeContextSize = beforeContextSize,
            maxIndentDepth = maxIndentDepth,
            agentController = agentController
        ))
    }
    
    /**
     * Create a new builder with the same configuration as this tracer
     */
    fun newBuilder(): Builder {
        return Builder()
            .filter(config.filter)
            .formatter(config.formatter)
            .enabled(config.enabled)
            .autoRetransform(config.autoRetransform)
            .traceEventGenerator(config.traceEventGenerator)
            .maxToStringLength(config.maxToStringLength)
            .beforeContextSize(config.beforeContextSize)
            .maxIndentDepth(config.maxIndentDepth)
            .codePathAgentController(config.agentController)
    }
}

// Default tracer instance
private val DEFAULT_TRACER = CodePathTracer.Builder().build()

/**
 * Primary API - simple usage with default settings
 */
fun <T> codePathTrace(block: () -> T): T = DEFAULT_TRACER.trace(block)

/**
 * Primary API - usage with custom tracer
 */
fun <T> codePathTrace(tracer: CodePathTracer, block: () -> T): T = tracer.trace(block)

