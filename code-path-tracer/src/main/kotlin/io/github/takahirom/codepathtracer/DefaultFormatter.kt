package io.github.takahirom.codepathtracer

/**
 * Default formatter for trace events with configurable max length and indent depth.
 */
object DefaultFormatter {
    @Volatile
    var defaultMaxLength = 30
    
    @Volatile
    var defaultMaxIndentDepth = 60
    
    fun format(event: TraceEvent): String = event.defaultFormat(defaultMaxLength, defaultMaxIndentDepth)
    fun format(event: TraceEvent, maxLength: Int): String = event.defaultFormat(maxLength, defaultMaxIndentDepth)
    fun format(event: TraceEvent, maxLength: Int, maxIndentDepth: Int): String = event.defaultFormat(maxLength, maxIndentDepth)
}