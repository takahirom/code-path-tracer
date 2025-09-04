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
 * val codePathTracerRule = CodePathTracer.Builder()
 *     .filter { event -> event.className.contains("MyClass") }
 *     .formatter { event -> "Custom: ${event.methodName}" }
 *     .asJUnitRule()
 *
 * // Or with custom filter/formatter
 * val customRule = CodePathTracer.Builder()
 *     .filter { event ->
 *         event.className.startsWith("com.example") &&
 *         event.depth < 5
 *     }
 *     .formatter { event ->
 *         "${" ".repeat(event.depth)}${event.fullMethodName}(${event.args.size})"
 *     }
 *     .asJUnitRule()
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