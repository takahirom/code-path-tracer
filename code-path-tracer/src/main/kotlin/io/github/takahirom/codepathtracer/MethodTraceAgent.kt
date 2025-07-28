package io.github.takahirom.codepathtracer

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassInjector
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.io.File
import java.lang.instrument.Instrumentation
import java.nio.file.Files

private const val DEBUG = false

/**
 * ByteBuddy automatic transformation agent (configurable version)
 */
object MethodTraceAgent {
    private const val DEBUG = false
    private var config: MethodTraceRule.Config? = null
    private var isInitialized = false


    fun initialize(config: MethodTraceRule.Config) {
        if (DEBUG) println("[MethodTrace] initialize() called. isInitialized=$isInitialized")

        if (isInitialized) {
            // Already initialized, just update config
            this.config = config
            if (DEBUG) println("[MethodTrace] Agent already initialized, updating config only")
            return
        }

        this.config = config
        if (DEBUG) println("[MethodTrace] Starting agent initialization with config: $config")

        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")
        if (DEBUG) println("[MethodTrace] Set ByteBuddy experimental property")

        try {
            if (DEBUG) println("[MethodTrace] Installing ByteBuddy agent...")
            val instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()
            if (DEBUG) println("[MethodTrace] ByteBuddy agent installed: $instrumentation")

            val agentBuilder = createAgentBuilder(config, instrumentation)
            if (DEBUG) println("[MethodTrace] Created AgentBuilder: $agentBuilder")

            agentBuilder
                .installOnByteBuddyAgent()
            if (DEBUG) println("[MethodTrace] AgentBuilder installed on instrumentation")

            isInitialized = true
            if (DEBUG) println("[MethodTrace] Agent initialization completed successfully")

        } catch (e: Exception) {
            if (DEBUG) {
                println("[MethodTrace] Agent initialization FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }




    @Suppress("NewApi")
    private fun createAgentBuilder(config: MethodTraceRule.Config, instrumentation: Instrumentation): AgentBuilder {
        if (DEBUG) println("[MethodTrace] createAgentBuilder called with config: $config")
        val temp = Files.createTempDirectory("tmp").toFile()
        fallbackToIndividualInjection(temp, instrumentation)
      return createAgentBuilderInstance()
    }

    private fun fallbackToIndividualInjection(temp: File, instrumentation: Instrumentation) {
        val classesToInject = mutableMapOf<TypeDescription.ForLoadedType, ByteArray?>()

        fun addClassAndDependencies(clazz: Class<*>) {
            try {
                classesToInject[TypeDescription.ForLoadedType(clazz)] = ClassFileLocator.ForClassLoader.read(clazz)
                if (DEBUG) println("[MethodTrace] Added class: ${clazz.name}")
            } catch (e: Exception) {
                if (DEBUG) println("[MethodTrace] Failed to add class ${clazz.name}: ${e.message}")
            }
        }

        addClassAndDependencies(MethodTraceAdvice::class.java)
        addClassAndDependencies(MethodTraceAdvice.Companion::class.java)
        addClassAndDependencies(MethodTraceAgent::class.java)
        addClassAndDependencies(MethodTraceRule.Config::class.java)
        addClassAndDependencies(TraceEvent::class.java)
        try {
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathtracer.DefaultFilter"))
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathtracer.DefaultFormatter"))
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Default filter/formatter classes not found " + e.stackTraceToString())
        }
        try {
            addClassAndDependencies(Class.forName("kotlin.jvm.internal.Intrinsics"))
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Kotlin intrinsics not found " + e.stackTraceToString())
        }

        ClassInjector.UsingInstrumentation
            .of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
            .inject(classesToInject)
    }

    private fun createAgentBuilderInstance(): AgentBuilder {
        return AgentBuilder.Default()
            .with(DebugListener())
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .ignore(
                ElementMatchers.nameStartsWith<NamedElement>("net.bytebuddy.")
                    .or(
                        ElementMatchers.nameStartsWith<NamedElement>("io.github.takahirom.codepathtracer.")
                            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("io.github.takahirom.codepathtracer.sample")))
                    )
            )
            .type(ElementMatchers.any<TypeDescription>())
            .transform(
                AgentBuilder.Transformer.ForAdvice()
                    .advice(
                        ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                            .and(ElementMatchers.not(ElementMatchers.isConstructor()))
                            .and(ElementMatchers.not(ElementMatchers.isTypeInitializer())),
                        MethodTraceAdvice::class.java.name
                    )
            )
    }

    fun getConfig(): MethodTraceRule.Config? = config

}

class DebugListener : AgentBuilder.Listener {
    override fun onDiscovery(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (DEBUG) println("[MethodTrace] Discovery: $typeName")
    }

    override fun onTransformation(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        dynamicType: net.bytebuddy.dynamic.DynamicType
    ) {
        if (DEBUG) println("[MethodTrace] Transformation: ${typeDescription.name}")
    }

    override fun onIgnored(
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (DEBUG) println("[MethodTrace] Ignored: ${typeDescription.name}")
    }

    override fun onError(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        throwable: Throwable
    ) {
        if (DEBUG) println("[MethodTrace] Error: $typeName - ${throwable.message}")
    }

    override fun onComplete(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean
    ) {
        if (DEBUG) println("[MethodTrace] Complete: $typeName")
    }
}
