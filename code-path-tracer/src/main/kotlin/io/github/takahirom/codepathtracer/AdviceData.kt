package io.github.takahirom.codepathtracer

/**
 * Raw advice data from ByteBuddy method interception
 */
sealed class AdviceData {
    data class Enter(
        val method: String,
        val args: Array<Any?>,
        val depth: Int
    ) : AdviceData() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Enter
            if (method != other.method) return false
            if (!args.contentEquals(other.args)) return false
            if (depth != other.depth) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = method.hashCode()
            result = 31 * result + args.contentHashCode()
            result = 31 * result + depth
            return result
        }
    }
    
    data class Exit(
        val method: String,
        val returnValue: Any?,
        val depth: Int
    ) : AdviceData()
}