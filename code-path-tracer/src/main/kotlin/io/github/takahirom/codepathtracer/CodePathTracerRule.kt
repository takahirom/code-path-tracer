package io.github.takahirom.codepathtracer

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Represents a method trace event with all relevant context information.
 */
sealed class TraceEvent {
    abstract val className: String
    abstract val methodName: String
    abstract val depth: Int
    
    val shortClassName: String get() = className.substringAfterLast('.')
    val fullMethodName: String get() = "$shortClassName.$methodName"
    
    /**
     * Method entry event
     */
    data class Enter(
        override val className: String,
        override val methodName: String,
        val args: Array<Any?>,
        override val depth: Int = 0,
    ) : TraceEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Enter
            if (className != other.className) return false
            if (methodName != other.methodName) return false
            if (!args.contentEquals(other.args)) return false
            if (depth != other.depth) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = className.hashCode()
            result = 31 * result + methodName.hashCode()
            result = 31 * result + args.contentHashCode()
            result = 31 * result + depth
            return result
        }
    }
    
    /**
     * Method exit event
     */
    data class Exit(
        override val className: String,
        override val methodName: String,
        val returnValue: Any? = null,
        override val depth: Int = 0,
    ) : TraceEvent()
    
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
        return when (this) {
            is Enter -> "$indent→ $fullMethodName(${args.size})"
            is Exit -> "$indent← $fullMethodName"
        }
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
 * val codePathTracerRule = CodePathTracerRule.builder()
 *     .packageIncludes("io.github.takahirom.codepathtracer")
 *     .methodExcludes("toString", "hashCode", "equals")
 *     .build()
 * 
 * // Or with custom filter/formatter
 * val customRule = CodePathTracerRule.builder()
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
class CodePathTracerRule private constructor(
    private val config: CodePathTracer.Config
) : TestRule {
    
    
    companion object {
        // DEBUG flag moved to CodePathTracer.DEBUG
        private var isAgentInstalled = false
        
        fun builder() = Builder()
        
        fun simple() = builder().build()
        
        // Static initialization to install agent early
        init {
            try {
                // Trigger CodePathTracerAgent class loading which will initialize the agent
                if (CodePathTracer.DEBUG) System.out.println("[MethodTrace] CodePathTracerRule static init - triggering agent")
                val defaultConfig = CodePathTracer.Config()
                // This will trigger CodePathTracerAgent's init block
                CodePathTracerAgent.initialize(defaultConfig)
                isAgentInstalled = true
                if (CodePathTracer.DEBUG) System.out.println("[MethodTrace] Agent installed via static initialization")
            } catch (e: Exception) {
                if (CodePathTracer.DEBUG) {
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
        
        
        fun build() = CodePathTracerRule(CodePathTracer.Config(filter, formatter, enabled))
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
            CodePathTracerAgent.initialize(config)
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                println("[MethodTrace] Failed to setup agent: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}