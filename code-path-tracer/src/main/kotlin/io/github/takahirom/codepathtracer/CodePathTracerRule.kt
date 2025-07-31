package io.github.takahirom.codepathtracer

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

object DefaultFilter {
    fun filter(event: TraceEvent): Boolean = true
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
class CodePathTracerRule internal constructor(
    private val config: CodePathTracer.Config
) : TestRule {


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