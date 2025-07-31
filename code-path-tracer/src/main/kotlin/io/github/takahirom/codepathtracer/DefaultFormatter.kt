package io.github.takahirom.codepathtracer

/**
 * Default formatter for trace events with configurable max length.
 */
object DefaultFormatter {
    @Volatile
    var defaultMaxLength = 30
    
    fun format(event: TraceEvent): String = event.defaultFormat(defaultMaxLength)
    fun format(event: TraceEvent, maxLength: Int): String = event.defaultFormat(maxLength)
}