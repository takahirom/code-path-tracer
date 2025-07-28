package io.github.takahirom.codepathfinder

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassInjector
import net.bytebuddy.implementation.bind.annotation.SuperCall
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.io.File
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.jar.JarFile


/**
 * ByteBuddy automatic transformation agent (configurable version)
 */
object MethodTraceAgent {
    private const val DEBUG = false
    private var config: MethodTraceRule.Config? = null
    private var isInitialized = false

    // Remove static initialization to avoid early class loading issues

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
    private fun createAgentBuilder(config: MethodTraceRule.Config, instrumentation: Instrumentation): net.bytebuddy.agent.builder.AgentBuilder {
        if (DEBUG) println("[MethodTrace] createAgentBuilder called with config: $config")
        val temp = Files.createTempDirectory("tmp").toFile()
        // Use individual class injection instead of JAR injection
        fallbackToIndividualInjection(temp, instrumentation)
      return createAgentBuilderInstance(config)
    }

    private fun <T:Any>addClass(
      clazz: Class<T>,
        instrumentation: Instrumentation,
        temp: File
    ) {
        try{
            // Handle both Bootstrap ClassLoader (null) and Application ClassLoader
            val classLoader = clazz.classLoader
            if (classLoader == null) {
                if (DEBUG) println("[MethodTrace] Processing Bootstrap ClassLoader class: ${clazz.name}")
                // For Bootstrap ClassLoader classes, try to get the JAR from protection domain
                val codeSource = clazz.protectionDomain?.codeSource
                if (codeSource != null) {
                    try {
                        val jarFile = File(codeSource.location.toURI())
                        if (jarFile.exists() && jarFile.name.endsWith(".jar")) {
                            instrumentation.appendToBootstrapClassLoaderSearch(JarFile(jarFile))
                            if (DEBUG) println("[MethodTrace] Added Bootstrap JAR: ${jarFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        if (DEBUG) println("[MethodTrace] Failed to add Bootstrap JAR for ${clazz.name}: ${e.message}")
                    }
                }
            } else {
                if (DEBUG) println("[MethodTrace] Processing Application ClassLoader class: ${clazz.name}")
                // For Application ClassLoader classes
                val codeSource = clazz.protectionDomain?.codeSource
                if (codeSource != null) {
                    val jarFile = File(codeSource.location.toURI())
                    if (DEBUG) println("[MethodTrace] Adding JAR to bootstrap classpath: ${jarFile.absolutePath}")

                    // Add JAR to Bootstrap ClassPath
                    instrumentation.appendToBootstrapClassLoaderSearch(JarFile(jarFile))
                    if (DEBUG) println("[MethodTrace] JAR successfully added to bootstrap classpath")
                }
            }
        } catch (e: Exception) {
            if (DEBUG) {
                println("[MethodTrace] Failed to add JAR for class ${clazz.name}: ${e.message}")
            }
        }
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

        // Add essential classes for method tracing
        addClassAndDependencies(MethodTraceAdvice::class.java)
        addClassAndDependencies(MethodTraceAdvice.Companion::class.java)
        addClassAndDependencies(MethodTraceAgent::class.java)
        addClassAndDependencies(MethodTraceRule.Config::class.java)
        addClassAndDependencies(TraceEvent::class.java)
        try {
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathfinder.DefaultFilter"))
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathfinder.DefaultFormatter"))
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Default filter/formatter classes not found")
        }
        try {
            addClassAndDependencies(Class.forName("kotlin.jvm.internal.Intrinsics"))
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Kotlin intrinsics not found")
        }

        ClassInjector.UsingInstrumentation
            .of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
            .inject(classesToInject)
    }

    private fun createAgentBuilderInstance(config: MethodTraceRule.Config): net.bytebuddy.agent.builder.AgentBuilder {
        return net.bytebuddy.agent.builder.AgentBuilder.Default()
            .with(object : net.bytebuddy.agent.builder.AgentBuilder.Listener {
                override fun onDiscovery(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean
                ) {
                    if (DEBUG && typeName.contains("Sample")) println("[MethodTrace] Discovery: $typeName")
                }
                override fun onTransformation(typeDescription: net.bytebuddy.description.type.TypeDescription, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean, dynamicType: net.bytebuddy.dynamic.DynamicType) {
                    if (DEBUG) println("[MethodTrace] Transformation: ${typeDescription.name}")
                }
                override fun onIgnored(typeDescription: net.bytebuddy.description.type.TypeDescription, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean) {
                    if (DEBUG && typeDescription.name.contains("Sample")) println("[MethodTrace] Ignored: ${typeDescription.name}")
                }
                override fun onError(typeName: String, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean, throwable: Throwable) {
                    if (DEBUG) println("[MethodTrace] Error: $typeName - ${throwable.message}")
                }
                override fun onComplete(typeName: String, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean) {
                    if (DEBUG && typeName.contains("Sample")) println("[MethodTrace] Complete: $typeName")
                }
            })
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .ignore(
                ElementMatchers.nameStartsWith<NamedElement>("net.bytebuddy.")
                    .or(
                        ElementMatchers.nameStartsWith<NamedElement>("io.github.takahirom.codepathfinder.")
                            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("io.github.takahirom.codepathfinder.sample")))
                    )
            )
            .type(createTypeMatcher(config))
            .transform(
                net.bytebuddy.agent.builder.AgentBuilder.Transformer.ForAdvice()
                    .advice(
                        ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                            .and(ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isConstructor()))
                            .and(ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isTypeInitializer())),
                        MethodTraceAdvice::class.java.name
                    )
            )
    }

    fun getConfig(): MethodTraceRule.Config? = config

    private fun createTypeMatcher(config: MethodTraceRule.Config): net.bytebuddy.matcher.ElementMatcher<in net.bytebuddy.description.type.TypeDescription> {
        return object : net.bytebuddy.matcher.ElementMatcher<net.bytebuddy.description.type.TypeDescription> {
            override fun matches(target: net.bytebuddy.description.type.TypeDescription): Boolean {
                if (DEBUG) println("[MethodTrace] Matching type: ${target.name}")
                return true
            }
        }
    }


}

object MyInterceptor {
    @Throws(java.lang.Exception::class)
    fun intercept(@SuperCall zuper: Callable<String?>): String? {
        println("Intercepted!")
        return zuper.call()
    }
}