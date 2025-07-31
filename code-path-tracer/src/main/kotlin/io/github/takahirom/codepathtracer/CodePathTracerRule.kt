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
                CodePathTracerCore.executeWithTracing(config) {
                    base.evaluate()
                }
            }
        }
    }

}