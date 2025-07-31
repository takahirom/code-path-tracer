package io.github.takahirom.codepathtracer

/**
 * Represents a method trace event with all relevant context information.
 */
sealed class TraceEvent {
    abstract val className: String
    abstract val methodName: String
    abstract val depth: Int

    val shortClassName: String get() = className.substringAfterLast('.')
    val fullMethodName: String get() = "$shortClassName.$methodName"

    companion object {
        // ThreadLocal guard to prevent infinite recursion during toString() calls
        private val isToStringCalling = ThreadLocal.withInitial { false }
        
        internal fun safeToString(obj: Any?, maxLength: Int = 30): String {
            if (isToStringCalling.get()) {
                return "recursion"
            }
            return try {
                isToStringCalling.set(true)
                when {
                    obj == null -> "null"
                    obj is Unit -> "Unit"
                    else -> {
                        val str = obj.toString()
                        val compressed = compressClassName(str)
                        compressed.take(maxLength)
                    }
                }
            } catch (e: Throwable) {
                // We need to catch all exception because some objects may throw AssertionError in toString()
                "error " + e.message.orEmpty().take(maxLength)
            } finally {
                isToStringCalling.set(false)
            }
        }
        
        /**
         * Compress package names in class names
         * Example: com.github.takahirom.MyClass -> c.g.t.MyClass
         */
        private fun compressClassName(str: String): String {
            // Match pattern like "com.github.takahirom" (2+ package segments)
            val packagePattern = Regex("""([a-z]+[a-z0-9]*\.){2,}""")
            
            return packagePattern.replace(str) { matchResult ->
                val packageName = matchResult.value
                // Split by dots and take first character of each segment except the last dot
                val segments = packageName.dropLast(1).split(".")
                segments.joinToString(".") { it.take(1) } + "."
            }
        }
    }

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
        val returnValue: Any?,
        override val depth: Int = 0,
    ) : TraceEvent()

    /**
     * Default formatting for trace events with configurable indent limits.
     */
    fun defaultFormat(maxLength: Int = 30): String {
        val indent = if (depth < 40) {
            "  ".repeat(depth)
        } else {
            " ".repeat(40) + "${depth}⇢"
        }
        return when (this) {
            is Enter -> {
                val argsStr = if (args.isNotEmpty()) {
                    args.joinToString(", ") { arg ->
                        safeToString(arg, maxLength)
                    }
                } else {
                    ""
                }
                "$indent→ $fullMethodName($argsStr)"
            }
            is Exit -> {
                // Don't show null or Unit return values (constructors and void methods)
                val returnPart = if (returnValue == null || returnValue is Unit) {
                    "" // Hide null/Unit returns (constructors and void methods)
                } else {
                    val returnStr = safeToString(returnValue, maxLength)
                    if (returnStr.isNotEmpty()) " = $returnStr" else ""
                }
                "$indent← $fullMethodName$returnPart"
            }
        }
    }
}