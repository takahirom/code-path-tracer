package io.github.takahirom.codepathtracer

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.lang.instrument.Instrumentation

/**
 * Global singleton holder for shared agent resources
 * Holds the actual Instrumentation and agent state that should be shared
 */
internal object CodePathAgentHolder {
    private var isInitialized = false
    private var resettableTransformer: net.bytebuddy.agent.builder.ResettableClassFileTransformer? = null
    private var instrumentation: Instrumentation? = null
    
    @Synchronized
    internal fun ensureInstalled(agentBuilder: AgentBuilder): Boolean {
        if (isInitialized) {
            if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[AgentHolder] Agent already installed")
            return true
        }
        
        if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[AgentHolder] Installing ByteBuddy Agent...")
        
        try {
            instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()
            resettableTransformer = agentBuilder.installOnByteBuddyAgent()
            isInitialized = true
            
            if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[AgentHolder] ByteBuddy Agent installed successfully")
            return true
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                CodePathTracer.getDebugLogger()("[AgentHolder] Agent installation FAILED: ${e.message}")
                CodePathTracer.getDebugLogger()(e.stackTraceToString())
            }
            return false
        }
    }
    
    @Synchronized
    internal fun reset() {
        try {
            val currentInstrumentation = instrumentation
            if (currentInstrumentation != null) {
                val resetSuccess = resettableTransformer?.reset(
                    currentInstrumentation,
                    AgentBuilder.RedefinitionStrategy.RETRANSFORMATION
                )
                if (CodePathTracer.DEBUG) {
                    CodePathTracer.getDebugLogger()("[AgentHolder] ByteBuddy transformer reset: $resetSuccess")
                }
            } else {
                if (CodePathTracer.DEBUG) {
                    CodePathTracer.getDebugLogger()("[AgentHolder] No instrumentation available for reset")
                }
            }
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                CodePathTracer.getDebugLogger()("[AgentHolder] Failed to reset transformer: ${e.message}")
            }
        }
        
        // Clean up resources in proper order - reset flag first to prevent new operations
        isInitialized = false
        resettableTransformer = null
        instrumentation = null
    }
    
    internal fun isAgentInstalled(): Boolean = isInitialized
}

/**
 * Controller for managing ByteBuddy agent configuration and lifecycle
 */
class CodePathAgentController private constructor(private val config: Config) {
    
    internal data class Config(
        val ignorePackages: List<String> = defaultIgnorePackages()
    ) {
        companion object {
            /**
             * Default packages to ignore during transformation
             */
            fun defaultIgnorePackages(): List<String> = listOf(
                "net.bytebuddy.",
                "java.",
                "kotlin.",
                "kotlinx.",
                "org.junit.",
                "sun.",
                "com.sun.",
                "android.util.DebugUtils",
                "io.github.takahirom.codepathtracer.",
                // IntelliJ IDEA debugger agent classes
                "com.intellij.rt.debugger.",
                "com.intellij.rt.execution.",
                // JetBrains debugger and profiler agents
                "com.jetbrains.jps.",
                "org.jetbrains.jps.",
                // JaCoCo code coverage agent classes
                "org.jacoco.",
                // Robolectric Android classes (always included for consistency)
                "android.view.View\$AttachInfo",
                "android.hardware.display.ColorDisplayManager",
                "android.hardware.display.",
                "android.view.ViewRootImpl",
                "android.view.Choreographer",
                "android.app.ActivityThread\$AppBindData",
                "android.app.Activity\$NonConfigurationInstances"
            )
        }
    }
    
    /**
     * Ensure ByteBuddy Agent is installed (heavy operation, once per process)
     */
    @Synchronized
    internal fun ensureInstalled() {
        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")
        
        // Delegate to AgentHolder for shared state management
        val agentBuilder = createAgentBuilder()
        CodePathAgentHolder.ensureInstalled(agentBuilder)
    }

    /**
     * Reset agent configuration and state
     */
    @Synchronized
    internal fun reset() {
        // Delegate to AgentHolder for shared state management
        CodePathAgentHolder.reset()
    }

    /**
     * Get the complete list of packages to ignore during transformation
     */
    internal fun getIgnorePackages(): List<String> = config.ignorePackages
    
    /**
     * Create ByteBuddy AgentBuilder with current configuration
     */
    private fun createAgentBuilder(): AgentBuilder {
        return AgentBuilder.Default()
            .with(DebugListener())
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            // Enable retransformation of already loaded classes
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .ignore(
                getIgnorePackages().fold(ElementMatchers.none<NamedElement>()) { matcher, pkg ->
                    matcher.or(ElementMatchers.nameStartsWith<NamedElement>(pkg))
                }
                    // jdk. causes StackOverflow - do not add
                    .or(ElementMatchers.nameContains<NamedElement>("\$\$Lambda\$"))
                    .or(ElementMatchers.nameContains<NamedElement>("\$lambda\$"))
                    .or(ElementMatchers.nameContains<NamedElement>("JvmMethodTraceTest\$methodTraceRule\$"))
                    // Ignore JDK internal reflection classes that cause ClassNotFoundException
                    .or(ElementMatchers.nameStartsWith<NamedElement>("jdk.internal.reflect.GeneratedConstructorAccessor"))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("jdk.internal.reflect.GeneratedMethodAccessor"))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("jdk.internal.reflect.GeneratedSerializationConstructorAccessor"))
            )
            .type(ElementMatchers.not(ElementMatchers.isInterface())
                .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                .and(ElementMatchers.not(ElementMatchers.isSynthetic())))
            .transform(
                AgentBuilder.Transformer.ForAdvice()
                    .advice(
                        ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                            .and(ElementMatchers.not(ElementMatchers.isTypeInitializer())),
                        MethodTraceAdvice::class.java.name
                    )
            )
    }
    
    
    class Builder {
        private var ignorePackages: List<String> = Config.defaultIgnorePackages()
        
        fun ignorePackages(packages: List<String>) = apply { 
            this.ignorePackages = packages 
        }
        
        fun build(): CodePathAgentController = CodePathAgentController(
            Config(ignorePackages)
        )
    }
    
    companion object {
        /**
         * Create a default agent controller
         */
        fun default(): CodePathAgentController = Builder().build()
    }
}

/**
 * Debug listener for ByteBuddy agent events
 */
private class DebugListener : AgentBuilder.Listener {
    override fun onDiscovery(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        // Method intentionally empty for performance
    }

    override fun onTransformation(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        dynamicType: net.bytebuddy.dynamic.DynamicType
    ) {
        if (CodePathTracer.DEBUG) CodePathTracer.getDebugLogger()("[MethodTrace] Transformation: ${typeDescription.name}")
    }

    override fun onIgnored(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        // Method intentionally empty for performance  
    }

    override fun onError(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        throwable: Throwable
    ) {
        if (CodePathTracer.DEBUG) {
            CodePathTracer.getDebugLogger()("[MethodTrace] Error: $typeName - ${throwable.message}")
            CodePathTracer.getDebugLogger()(throwable.stackTraceToString())
        }
    }

    override fun onComplete(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        // Method intentionally empty for performance
    }
}