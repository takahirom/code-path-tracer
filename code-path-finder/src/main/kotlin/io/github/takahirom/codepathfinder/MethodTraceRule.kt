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
    val timestamp: Long = System.currentTimeMillis(),
    val threadName: String = Thread.currentThread().name
) {
    val shortClassName: String = className.substringAfterLast('.')
    val fullMethodName: String = "$shortClassName.$methodName"
    
    /**
     * Default formatting for trace events.
     */
    fun defaultFormat(): String = 
        "${" ".repeat(depth)}→ $fullMethodName(${args.size})"
    
    /**
     * Minimal formatting showing only method name.
     */
    fun minimal(): String = methodName
    
    /**
     * Detailed formatting with arguments and return value.
     */
    fun detailed(): String {
        val argsStr = if (args.isEmpty()) "" else "(${args.joinToString { it?.toString()?.take(10) ?: "null" }})"
        val returnStr = returnValue?.let { " -> ${it.toString().take(20)}" } ?: ""
        return "$fullMethodName$argsStr$returnStr"
    }
    
    /**
     * Indented formatting for call hierarchy visualization.
     */
    fun indented(): String = 
        "${" ".repeat(depth)}→ $fullMethodName(${args.size})"
        
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TraceEvent
        return className == other.className &&
                methodName == other.methodName &&
                args.contentEquals(other.args) &&
                returnValue == other.returnValue &&
                depth == other.depth
    }
    
    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + methodName.hashCode()
        result = 31 * result + args.contentHashCode()
        result = 31 * result + (returnValue?.hashCode() ?: 0)
        result = 31 * result + depth
        return result
    }
}

/**
 * ByteBuddyを使った自動メソッドトレース用のJUnit Rule
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
        val filter: (TraceEvent) -> Boolean = { true },
        val formatter: (TraceEvent) -> String = TraceEvent::defaultFormat,
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
        private var filter: (TraceEvent) -> Boolean = { true }
        private var formatter: (TraceEvent) -> String = TraceEvent::defaultFormat
        private var enabled = true
        
        fun filter(predicate: (TraceEvent) -> Boolean) = apply { filter = predicate }
        fun formatter(format: (TraceEvent) -> String) = apply { formatter = format }
        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        
        // Convenience methods for common patterns
        fun packageIncludes(vararg packages: String) = apply {
            filter = { event -> packages.any { event.className.startsWith(it) } }
        }
        
        fun packageExcludes(vararg packages: String) = apply {
            val currentFilter = filter
            filter = { event -> currentFilter(event) && packages.none { event.className.contains(it) } }
        }
        
        fun methodExcludes(vararg methods: String) = apply {
            val currentFilter = filter
            filter = { event -> currentFilter(event) && event.methodName !in methods }
        }
        
        fun maxDepth(depth: Int) = apply {
            val currentFilter = filter
            filter = { event -> currentFilter(event) && event.depth <= depth }
        }
        
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