package io.github.takahirom.codepathtracer

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassInjector
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.io.File
import java.lang.instrument.Instrumentation
import java.nio.file.Files

// DEBUG flag moved to CodePathTracer.DEBUG

/**
 * ByteBuddy automatic transformation agent (configurable version)
 */
object CodePathTracerAgent {
    // DEBUG flag moved to CodePathTracer.DEBUG
    private var config: CodePathTracer.Config? = null
    private var isInitialized = false
    private var resettableTransformer: net.bytebuddy.agent.builder.ResettableClassFileTransformer? = null


    fun initialize(config: CodePathTracer.Config) {
        if (CodePathTracer.DEBUG) println("[MethodTrace] initialize() called. isInitialized=$isInitialized")

        if (isInitialized) {
            // Already initialized, just update config
            this.config = config
            if (CodePathTracer.DEBUG) println("[MethodTrace] Agent already initialized, updating config only")
            return
        }

        this.config = config
        if (CodePathTracer.DEBUG) println("[MethodTrace] Starting agent initialization with config: $config")

        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")
        if (CodePathTracer.DEBUG) println("[MethodTrace] Set ByteBuddy experimental property")

        try {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Installing ByteBuddy agent...")
            val instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()
            if (CodePathTracer.DEBUG) println("[MethodTrace] ByteBuddy agent installed: $instrumentation")

            val agentBuilder = createAgentBuilder(config, instrumentation)
            if (CodePathTracer.DEBUG) println("[MethodTrace] Created AgentBuilder: $agentBuilder")

            resettableTransformer = agentBuilder
                .installOnByteBuddyAgent()
            if (CodePathTracer.DEBUG) println("[MethodTrace] AgentBuilder installed on instrumentation")
            
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
            if (CodePathTracer.DEBUG) println("[MethodTrace] Agent initialization completed successfully")

        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) {
                println("[MethodTrace] Agent initialization FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }




    @Suppress("NewApi")
    private fun createAgentBuilder(config: CodePathTracer.Config, instrumentation: Instrumentation): AgentBuilder {
        if (CodePathTracer.DEBUG) println("[MethodTrace] createAgentBuilder called with config: $config")
        val temp = Files.createTempDirectory("tmp").toFile()
        fallbackToIndividualInjection(temp, instrumentation)
      return createAgentBuilderInstance()
    }

    private fun fallbackToIndividualInjection(temp: File, instrumentation: Instrumentation) {
        val classesToInject = mutableMapOf<TypeDescription.ForLoadedType, ByteArray>()

        fun addClassAndDependencies(clazz: Class<*>) {
            try {
                val classBytes = ClassFileLocator.ForClassLoader.read(clazz)
                if (classBytes != null) {
                    classesToInject[TypeDescription.ForLoadedType(clazz)] = classBytes
                    if (CodePathTracer.DEBUG) println("[MethodTrace] Added class: ${clazz.name}")
                } else {
                    if (CodePathTracer.DEBUG) println("[MethodTrace] Failed to read class bytes for: ${clazz.name}")
                }
            } catch (e: Exception) {
                if (CodePathTracer.DEBUG) println("[MethodTrace] Failed to add class ${clazz.name}: ${e.message}")
            }
        }

        addClassAndDependencies(MethodTraceAdvice::class.java)
        addClassAndDependencies(MethodTraceAdvice.Companion::class.java)
        addClassAndDependencies(CodePathTracerAgent::class.java)
        addClassAndDependencies(CodePathTracer.Config::class.java)
        addClassAndDependencies(TraceEvent::class.java)
        try {
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathtracer.DefaultFilter"))
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathtracer.DefaultFormatter"))
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Default filter/formatter classes not found " + e.stackTraceToString())
        }
        try {
            addClassAndDependencies(Class.forName("kotlin.jvm.internal.Intrinsics"))
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Kotlin intrinsics not found " + e.stackTraceToString())
        }

        try {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Injecting ${classesToInject.size} classes to Bootstrap ClassPath")
            ClassInjector.UsingInstrumentation
                .of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
                .inject(classesToInject)
            if (CodePathTracer.DEBUG) println("[MethodTrace] Successfully injected classes to Bootstrap ClassPath")
        } catch (e: Exception) {
            if (CodePathTracer.DEBUG) println("[MethodTrace] Failed to inject classes: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createAgentBuilderInstance(): AgentBuilder {
        return AgentBuilder.Default()
            .with(DebugListener())
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            // Enable retransformation of already loaded classes
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .ignore(
                ElementMatchers.nameStartsWith<NamedElement>("net.bytebuddy.")
                    .or(ElementMatchers.nameStartsWith<NamedElement>("java."))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("kotlin."))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("org.junit."))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("sun."))
                    // jdk. causes StackOverflow - do not add
                    .or(ElementMatchers.nameStartsWith<NamedElement>("com.sun."))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("android.util.DebugUtils"))
                    .or(ElementMatchers.nameStartsWith<NamedElement>("io.github.takahirom.codepathtracer."))
                    .or(ElementMatchers.nameContains<NamedElement>("\$\$Lambda\$"))
                    .or(ElementMatchers.nameContains<NamedElement>("\$lambda\$"))
                    .or(ElementMatchers.nameContains<NamedElement>("JvmMethodTraceTest\$methodTraceRule\$"))
            )
            .type(ElementMatchers.not(ElementMatchers.isInterface())
                .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                .and(ElementMatchers.not(ElementMatchers.isSynthetic())))
            .transform(
                AgentBuilder.Transformer.ForAdvice()
                    .advice(
                        ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                            .and(ElementMatchers.not(ElementMatchers.isTypeInitializer())),
                            // Include constructors for tracing
                        MethodTraceAdvice::class.java.name
                    )
            )
    }

    fun getConfig(): CodePathTracer.Config? = config
    
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
                
                // Test if this class would match the current filter
                if (wouldMatchFilter(clazz, config)) {
                    candidates.add(clazz)
                    if (CodePathTracer.DEBUG) {
                        println("[MethodTrace] Candidate found: ${clazz.name} (reason: filter match)")
                    }
                }
                
                // Look for inner classes that might be interesting
                if (hasInnerClassesOfInterest(clazz, config)) {
                    candidates.add(clazz)
                    if (CodePathTracer.DEBUG) {
                        println("[MethodTrace] Candidate found: ${clazz.name} (reason: inner classes)")
                    }
                }
                
            } catch (_: Exception) {
                // Skip problematic classes silently
            }
        }
        
        return candidates.distinct()
    }
    
    private fun wouldBeIgnored(className: String): Boolean {
        return className.startsWith("net.bytebuddy.") ||
               className.startsWith("java.") ||
               className.startsWith("kotlin.") ||
               className.startsWith("org.junit.") ||
               className.startsWith("sun.") ||
               className.startsWith("com.sun.") ||
               className.startsWith("android.util.DebugUtils") ||
               className.startsWith("io.github.takahirom.codepathtracer.") ||
               className.contains("\$\$Lambda\$") ||
               className.contains("\$lambda\$") ||
               className.contains("JvmMethodTraceTest\$methodTraceRule\$")
    }
    
    private fun wouldMatchFilter(clazz: Class<*>, config: CodePathTracer.Config): Boolean {
        try {
            // Create a dummy trace event to test the filter
            val dummyEvent = TraceEvent.Enter(
                className = clazz.name,
                methodName = "testMethod",
                args = emptyArray(),
                depth = 0
            )
            return config.filter(dummyEvent)
        } catch (_: Exception) {
            return false
        }
    }
    
    private fun hasInnerClassesOfInterest(clazz: Class<*>, @Suppress("UNUSED_PARAMETER") config: CodePathTracer.Config): Boolean {
        // Look for inner classes ($) that might be of interest
        return clazz.name.contains("$") && 
               !clazz.name.contains("Lambda") &&
               !clazz.name.contains("lambda") &&
               !clazz.name.contains("methodTraceRule")
    }
    
    /**
     * Reset configuration to disable tracing
     */
    fun reset() {
        if (CodePathTracer.DEBUG) println("[MethodTrace] Resetting configuration")
        config = null
        
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
            if (CodePathTracer.DEBUG) println("[MethodTrace] ByteBuddy transformer reset failed: ${e.message}")
        }
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
        if (CodePathTracer.DEBUG) println("[MethodTrace] Error: $typeName - ${throwable.message}")
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
