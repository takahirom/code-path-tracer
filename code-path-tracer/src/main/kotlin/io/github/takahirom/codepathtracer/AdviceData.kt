package io.github.takahirom.codepathtracer

/**
 * Raw advice data from ByteBuddy method interception using reliable @Origin information
 */
sealed class AdviceData {
    data class Enter(
        val args: Array<Any?>,
        val depth: Int,
        val clazz: Class<*>,       // Reliable class information from ByteBuddy @Origin
        val methodName: String,    // Reliable method name from @Origin("#m") - <init> for constructors
        val descriptor: String     // Method descriptor from @Origin("#d")
    ) : AdviceData() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Enter
            if (!args.contentEquals(other.args)) return false
            if (depth != other.depth) return false
            if (clazz != other.clazz) return false
            if (methodName != other.methodName) return false
            if (descriptor != other.descriptor) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = args.contentHashCode()
            result = 31 * result + depth
            result = 31 * result + clazz.hashCode()
            result = 31 * result + methodName.hashCode()
            result = 31 * result + descriptor.hashCode()
            return result
        }
    }
    
    data class Exit(
        val returnValue: Any?,
        val depth: Int,
        val clazz: Class<*>,
        val methodName: String,
        val descriptor: String
    ) : AdviceData()
}