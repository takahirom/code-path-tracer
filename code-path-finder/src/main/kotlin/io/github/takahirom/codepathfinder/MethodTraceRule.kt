package io.github.takahirom.codepathfinder

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Represents a method trace event with all relevant context information.
 */
data class TraceEvent(
    val className: String,
    val methodName: String,
    val args: Array<Any?>,
    val returnValue: Any? = null,
    val depth: Int = 0,
) {
    val shortClassName: String = className.substringAfterLast('.')
    val fullMethodName: String = "$shortClassName.$methodName"
    
    /**
     * Default formatting for trace events.
     */
    fun defaultFormat(): String {
        val indent = when (depth) {
            0 -> ""
            1 -> " "
            2 -> "  "
            3 -> "   "
            4 -> "    "
            5 -> "     "
            6 -> "      "
            7 -> "       "
            8 -> "        "
            9 -> "         "
            10 -> "          "
            else -> "           " // max 10+ levels
        }
        return "$indent→ $fullMethodName(${args.size})"
    }
    
        
}

object DefaultFilter {
    fun filter(event: TraceEvent): Boolean = true
}

object DefaultFormatter {
    fun format(event: TraceEvent): String = event.defaultFormat()
}

/**
 * JUnit Rule for automatic method tracing using ByteBuddy
 * 
 * Usage:
 * ```
 * @get:Rule
 * val methodTraceRule = MethodTraceRule.builder()
 *     .packageIncludes("io.github.takahirom.codepathfinder")
 *     .methodExcludes("toString", "hashCode", "equals")
 *     .build()
 * 
 * // Or with custom filter/formatter
 * val customRule = MethodTraceRule.builder()
 *     .filter { event -> 
 *         event.className.startsWith("com.example") && 
 *         event.depth < 5 
 *     }
 *     .formatter { event -> 
 *         "${" ".repeat(event.depth)}${event.fullMethodName}(${event.args.size})" 
 *     }
 *     .build()
 * ```
 */
class MethodTraceRule private constructor(
    private val config: Config
) : TestRule {
    
    data class Config(
        val filter: (TraceEvent) -> Boolean = DefaultFilter::filter,
        val formatter: (TraceEvent) -> String = DefaultFormatter::format,
        val enabled: Boolean = true
    )
    
    companion object {
        private const val DEBUG = false
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
        private var filter: (TraceEvent) -> Boolean = { true }
        private var formatter: (TraceEvent) -> String = TraceEvent::defaultFormat
        private var enabled = true
        
        fun filter(predicate: (TraceEvent) -> Boolean) = apply { filter = predicate }
        fun formatter(format: (TraceEvent) -> String) = apply { formatter = format }
        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        
        
        fun build() = MethodTraceRule(Config(filter, formatter, enabled))
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