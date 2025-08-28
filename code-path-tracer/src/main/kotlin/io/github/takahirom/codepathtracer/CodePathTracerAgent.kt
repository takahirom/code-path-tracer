package io.github.takahirom.codepathtracer

/**
 * ByteBuddy automatic transformation agent (delegating to CodePathAgentController)
 */
object CodePathTracerAgent {
    private var config: CodePathTracer.Config? = null
    
    // Singleton instance to maintain state across calls
    private val defaultAgentController = CodePathAgentController.default()

    /**
     * Ensure ByteBuddy Agent is installed (heavy operation, once per process)
     */
    @Synchronized
    fun ensureInstalled() {
        val agentController = config?.agentController ?: defaultAgentController
        agentController.ensureInstalled()
    }
    
    /**
     * Update configuration (lightweight operation)
     */
    @Synchronized
    internal fun updateConfig(newConfig: CodePathTracer.Config?) {
        this.config = newConfig
        
        if (newConfig != null) {
            DefaultFormatter.defaultMaxLength = newConfig.maxToStringLength
            if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[MethodTrace] Config updated: $newConfig")
        } else {
            if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[MethodTrace] Config cleared (tracing disabled)")
        }
    }


    @Synchronized
    internal fun getConfig(): CodePathTracer.Config? = config
    
    
    
    /**
     * Reset only configuration to disable tracing (no ByteBuddy retransform)
     */
    @Synchronized
    internal fun resetConfigOnly() {
        config = null
        
        // Clean up ThreadLocal variables to prevent memory leaks
        try {
            MethodTraceAdvice.cleanup()
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                CodePathTracer.getDebugLogger()("[TracerAgent] Failed to cleanup ThreadLocal: ${e.message}")
            }
        }
    }
    
    /**
     * Reset configuration to disable tracing
     */
    @Synchronized
    fun reset() {
        val agentController = config?.agentController ?: defaultAgentController
        
        config = null
        
        // Clean up ThreadLocal variables to prevent memory leaks
        try {
            MethodTraceAdvice.cleanup()
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                CodePathTracer.getDebugLogger()("[TracerAgent] Failed to cleanup ThreadLocal: ${e.message}")
            }
        }
        
        // Delegate reset to agent controller
        agentController.reset()
    }

}
