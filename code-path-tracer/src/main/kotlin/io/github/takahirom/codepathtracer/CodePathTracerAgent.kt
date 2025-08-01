package io.github.takahirom.codepathtracer

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.lang.instrument.Instrumentation

// DEBUG flag moved to CodePathTracer.DEBUG

/**
 * ByteBuddy automatic transformation agent (configurable version)
 */
object CodePathTracerAgent {
    // DEBUG flag moved to CodePathTracer.DEBUG
    private var config: CodePathTracer.Config? = null
    private var isInitialized = false
    private var resettableTransformer: net.bytebuddy.agent.builder.ResettableClassFileTransformer? = null

    /**
     * Ensure ByteBuddy Agent is installed (heavy operation, once per process)
     */
    @Synchronized
    fun ensureInstalled() {
        if (isInitialized) {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Agent already installed")
            return
        }
        
        if (CodePathTracer.DEBUG) println("[MethodTrace] Installing ByteBuddy Agent...")
        
        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")
        
        try {
            val instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()
            val agentBuilder = createAgentBuilder()
            
            resettableTransformer = agentBuilder.installOnByteBuddyAgent()
            isInitialized = true
            
            if (CodePathTracer.DEBUG) println("[MethodTrace] ByteBuddy Agent installed successfully")
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                println("[MethodTrace] Agent installation FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update configuration (lightweight operation)
     */
    @Synchronized
    fun updateConfig(newConfig: CodePathTracer.Config?) {
        this.config = newConfig
        
        if (newConfig != null) {
            DefaultFormatter.defaultMaxLength = newConfig.maxToStringLength
            if (CodePathTracer.DEBUG) println("[MethodTrace] Config updated: $newConfig")
        } else {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Config cleared (tracing disabled)")
        }
    }

    @Synchronized
    fun initialize(config: CodePathTracer.Config) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] initialize() called. isInitialized=$isInitialized")

        if (isInitialized) {
            // Already initialized, just update config
            this.config = config
            DefaultFormatter.defaultMaxLength = config.maxToStringLength
            DefaultFormatter.defaultMaxIndentDepth = config.maxIndentDepth
            if (CodePathTracer.DEBUG) println("[MethodTrace] Agent already initialized, updating config only")
            return
        }

        this.config = config
        DefaultFormatter.defaultMaxLength = config.maxToStringLength
        DefaultFormatter.defaultMaxIndentDepth = config.maxIndentDepth
        if (CodePathTracer.DEBUG) println("[MethodTrace] Starting agent initialization with config: $config")

        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")

        try {
            val instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()
            val agentBuilder = createAgentBuilder()

            resettableTransformer = agentBuilder
                .installOnByteBuddyAgent()
            
            // Auto-detect and retransform already loaded classes that might need tracing
            if (config.autoRetransform) {
                try {
                    val candidates = findRetransformCandidates(instrumentation, config)
                    if (candidates.isNotEmpty()) {
                        if (CodePathTracer.DEBUG) {
                            println("[MethodTrace] Auto-detected ${candidates.size} retransform candidates:")
                            candidates.forEach { clazz ->
                                println("[MethodTrace]   - ${clazz.name}")
                            }
                        }
                        instrumentation.retransformClasses(*candidates.toTypedArray())
                    }
                } catch (e: Exception) {
                    if (CodePathTracer.DEBUG) {
                        println("[MethodTrace] Failed to retransform candidates: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }

            isInitialized = true

        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                println("[MethodTrace] Agent initialization FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }




    @Suppress("NewApi")
    private fun createAgentBuilder(): AgentBuilder {
        return AgentBuilder.Default()
            .with(DebugListener())
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            // Enable retransformation of already loaded classes
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .ignore(
                ignorePackages().fold(ElementMatchers.none<NamedElement>()) { matcher, pkg ->
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

    @Synchronized
    fun getConfig(): CodePathTracer.Config? = config
    
    /**
     * Common list of package prefixes to ignore during tracing
     */
    private fun ignorePackages(): List<String> {
        val basePackages = listOf(
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
            "org.jetbrains.jps."
        )
        
        // In Robolectric sandbox environments, exclude problematic android classes that cause AccessError
        // These are classes that Robolectric's Reflectors cannot access due to ClassLoader isolation
        return if (isRobolectricSandboxEnvironment()) {
            basePackages + listOf(
                "android.view.View\$AttachInfo",
                "android.hardware.display.ColorDisplayManager",
                "android.hardware.display.",
                "android.view.ViewRootImpl",
                "android.view.Choreographer"
            )
        } else {
            basePackages
        }
    }
    
    /**
     * Auto-detect classes that should be retransformed for tracing
     */
    private fun findRetransformCandidates(instrumentation: Instrumentation, config: CodePathTracer.Config): List<Class<*>> {
        if (!instrumentation.isRetransformClassesSupported) {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Retransformation not supported")
            return emptyList()
        }
        
        val loadedClasses = instrumentation.allLoadedClasses
        val candidates = mutableListOf<Class<*>>()
        
        for (clazz in loadedClasses) {
            try {
                // Skip if not modifiable
                if (!instrumentation.isModifiableClass(clazz)) continue
                
                // Skip classes that would be ignored anyway
                if (wouldBeIgnored(clazz.name)) continue
                
                // Add class as candidate (filtering will happen at method level)
                candidates.add(clazz)

                
            } catch (_: Exception) {
                // Skip problematic classes silently
            }
        }
        
        return candidates.distinct()
    }
    
    private fun wouldBeIgnored(className: String): Boolean {
        return ignorePackages().any { className.startsWith(it) } ||
               className.contains("\$\$Lambda\$") ||
               className.contains("\$lambda\$") ||
               className.contains("JvmMethodTraceTest\$methodTraceRule\$")
    }
    
    
    
    
    private fun isRobolectricSandboxEnvironment(): Boolean {
        return try {
            val contextClassLoader = Thread.currentThread().contextClassLoader
            contextClassLoader?.toString()?.let { classLoaderName ->
                classLoaderName.contains("AndroidSandbox") || classLoaderName.contains("SdkSandboxClassLoader")
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reset only configuration to disable tracing (no ByteBuddy retransform)
     */
    @Synchronized
    fun resetConfigOnly() {
        config = null
        
        // Clean up ThreadLocal variables to prevent memory leaks
        try {
            MethodTraceAdvice.cleanup()
        } catch (e: Exception) {
        }
    }
    
    /**
     * Reset configuration to disable tracing
     */
    @Synchronized
    fun reset() {
        config = null
        
        // Clean up ThreadLocal variables to prevent memory leaks
        try {
            MethodTraceAdvice.cleanup()
        } catch (e: Exception) {
        }
        
        // Reset the ByteBuddy transformer
        try {
            val resetSuccess = resettableTransformer?.reset(
                net.bytebuddy.agent.ByteBuddyAgent.getInstrumentation(),
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION
            )
            if (CodePathTracer.DEBUG) {
                if (resetSuccess == true) {
                    println("[MethodTrace] ByteBuddy transformer reset successfully")
                } else {
                    println("[MethodTrace] ByteBuddy transformer reset returned: $resetSuccess")
                }
            }
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                println("[MethodTrace] Failed to reset ByteBuddy transformer: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // Force reset initialization state to allow re-initialization 
        isInitialized = false
        resettableTransformer = null
        
    }

}

class DebugListener : AgentBuilder.Listener {
    override fun onDiscovery(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] Discovery: $typeName")
    }

    override fun onTransformation(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        dynamicType: net.bytebuddy.dynamic.DynamicType
    ) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] Transformation: ${typeDescription.name}")
    }

    override fun onIgnored(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] Ignored: ${typeDescription.name}")
    }

    override fun onError(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        throwable: Throwable
    ) {
        if (CodePathTracer.DEBUG) {
            println("[MethodTrace] Error: $typeName - ${throwable.message}")
            throwable.printStackTrace()
        }
    }

    override fun onComplete(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] Complete: $typeName")
    }
}
