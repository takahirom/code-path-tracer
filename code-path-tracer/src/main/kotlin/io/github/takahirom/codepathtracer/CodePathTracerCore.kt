package io.github.takahirom.codepathtracer

/**
 * Core tracing logic shared between Rule and CodePathTracer
 * Handles Agent lifecycle management with proper separation of concerns
 */
object CodePathTracerCore {
    
    /**
     * Execute code block with tracing enabled
     * This is the central method that handles all tracing lifecycle
     */
    fun <T> executeWithTracing(config: CodePathTracer.Config, block: () -> T): T {
        if (!config.enabled) {
            return block()
        }
        
        // Ensure Agent is installed (heavy operation, once per process)
        CodePathTracerAgent.ensureInstalled()
        
        // Update configuration (lightweight operation)
        CodePathTracerAgent.updateConfig(config)
        
        return try {
            block()
        } finally {
            // Clean up: disable tracing and clean ThreadLocal variables
            CodePathTracerAgent.updateConfig(null)
            try {
                MethodTraceAdvice.cleanup()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}