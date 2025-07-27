package io.github.takahirom.codepathfinder

/**
 * ByteBuddy Advice用の静的クラス（設定可能版）
 */
class MethodTraceAdvice {
    companion object {
        private val depthCounter = ThreadLocal.withInitial { 0 }
        
        @JvmStatic
        @net.bytebuddy.asm.Advice.OnMethodEnter
        fun methodEnter(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.AllArguments args: Array<Any?>) {
            
            val config = MethodTraceAgent.getConfig() ?: return
            
            val depth = depthCounter.get() ?: 0
            val indent = " ".repeat(depth)
            
            val shortMethod = method
                .replace("public static final ", "")
                .replace("public final ", "")
                .replace("private final ", "")
                .replace("static final ", "")
                .replace("public ", "")
                .replace("private ", "")
                .replace("static ", "")
                .let { cleaned ->
                    val methodPart = cleaned.substringBefore("(")
                    val parts = methodPart.split(".")
                    if (parts.size >= 2) {
                        "${parts[parts.size - 2]}.${parts.last()}"
                    } else {
                        methodPart
                    }
                }
            
            // Method exclusion check
            val methodName = shortMethod.substringAfterLast(".")
            if (config.methodExcludes.contains(methodName)) {
                depthCounter.set(depth + 1)  // Update depth but don't log
                return
            }
            
            val argsStr = if (!config.showArguments || args.isEmpty()) {
                ""
            } else {
                val argsList = args.mapIndexed { index, arg ->
                    val argValue = arg?.toString() ?: "null"
                    val cleanValue = if (argValue.contains('.') && argValue.matches(Regex("^[a-zA-Z][a-zA-Z0-9.]*[a-zA-Z0-9]$"))) {
                        argValue.substringAfterLast('.')
                    } else {
                        argValue
                    }.take(config.argMaxLength)
                    "arg$index=$cleanValue"
                }.joinToString(", ")
                "($argsList)"
            }
            
            println("[MethodTrace] $indent→ ENTERING: $shortMethod$argsStr")
            depthCounter.set(depth + 1)
        }
        
        @JvmStatic  
        @net.bytebuddy.asm.Advice.OnMethodExit
        fun methodExit(@net.bytebuddy.asm.Advice.Origin method: String, @net.bytebuddy.asm.Advice.Return returnValue: Any?) {
            val config = MethodTraceAgent.getConfig() ?: return
            
            val depth = (depthCounter.get() ?: 1) - 1
            depthCounter.set(depth)
            val indent = " ".repeat(depth)
            
            val shortMethod = method
                .replace("public static final ", "")
                .replace("public final ", "")
                .replace("private final ", "")
                .replace("static final ", "")
                .replace("public ", "")
                .replace("private ", "")
                .replace("static ", "")
                .let { cleaned ->
                    val methodPart = cleaned.substringBefore("(")
                    val parts = methodPart.split(".")
                    if (parts.size >= 2) {
                        "${parts[parts.size - 2]}.${parts.last()}"
                    } else {
                        methodPart
                    }
                }
            
            // Method exclusion check for exit as well
            val methodName = shortMethod.substringAfterLast(".")
            if (config.methodExcludes.contains(methodName)) {
                return
            }
            
            val returnStr = if (!config.showReturns) {
                ""
            } else if (returnValue == null) {
                " -> null"
            } else if (returnValue::class.java.name == "void" || returnValue::class.java.name == "java.lang.Void") {
                ""
            } else {
                " -> ${returnValue.toString().take(config.returnMaxLength)}"
            }
            
            println("[MethodTrace] $indent← EXITING: $shortMethod$returnStr")
        }
    }
}