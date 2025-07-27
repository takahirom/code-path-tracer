package io.github.takahirom.codepathfinder

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * ByteBuddyを使った自動メソッドトレース用のJUnit Rule
 * 
 * Usage:
 * ```
 * @get:Rule
 * val methodTraceRule = MethodTraceRule.builder()
 *     .packageIncludes("org.example")
 *     .argMaxLength(10)
 *     .returnMaxLength(20)
 *     .showArguments(true)
 *     .showReturns(true)
 *     .build()
 * ```
 */
class MethodTraceRule private constructor(
    private val config: Config
) : TestRule {
    
    data class Config(
        val packageIncludes: List<String> = listOf("org.example"),
        val packageExcludes: List<String> = emptyList(),
        val methodExcludes: List<String> = listOf("toString", "hashCode", "equals"),
        val argMaxLength: Int = 10,
        val returnMaxLength: Int = 20,
        val showArguments: Boolean = true,
        val showReturns: Boolean = true,
        val enabled: Boolean = true
    )
    
    companion object {
        private const val DEBUG = true
        private var isAgentInstalled = false
        
        fun builder() = Builder()
        
        fun simple() = builder().build()
        
        // Static initialization to install agent early
        init {
            try {
                // Trigger MethodTraceAgent class loading which will initialize the agent
                if (DEBUG) System.out.println("[MethodTrace] MethodTraceRule static init - triggering agent")
                val defaultConfig = Config()
                // This will trigger MethodTraceAgent's init block
                MethodTraceAgent.initialize(defaultConfig)
                isAgentInstalled = true
                if (DEBUG) System.out.println("[MethodTrace] Agent installed via static initialization")
            } catch (e: Exception) {
                if (DEBUG) {
                    System.out.println("[MethodTrace] Static agent setup failed: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    class Builder {
        private var packageIncludes = listOf("org.example")
        private var packageExcludes = emptyList<String>()
        private var methodExcludes = listOf("toString", "hashCode", "equals")
        private var argMaxLength = 10
        private var returnMaxLength = 20
        private var showArguments = true
        private var showReturns = true
        private var enabled = true
        
        fun packageIncludes(vararg packages: String) = apply { packageIncludes = packages.toList() }
        fun packageExcludes(vararg packages: String) = apply { packageExcludes = packages.toList() }
        fun methodExcludes(vararg methods: String) = apply { methodExcludes = methods.toList() }
        
        fun argMaxLength(length: Int) = apply { argMaxLength = length }
        fun returnMaxLength(length: Int) = apply { returnMaxLength = length }
        fun showArguments(show: Boolean) = apply { showArguments = show }
        fun showReturns(show: Boolean) = apply { showReturns = show }
        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        
        fun build() = MethodTraceRule(Config(
            packageIncludes, packageExcludes, methodExcludes,
            argMaxLength, returnMaxLength, showArguments, showReturns, enabled
        ))
    }
    
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (!config.enabled) {
                    base.evaluate()
                    return
                }
                
                setupAgent()
                try {
                    base.evaluate()
                } finally {
                    // cleanup if needed
                }
            }
        }
    }
    
    private fun setupAgent() {
        try {
            // Update agent config (agent is already installed via static init)
            MethodTraceAgent.initialize(config)
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Failed to setup agent: ${e.message}")
        }
    }
}