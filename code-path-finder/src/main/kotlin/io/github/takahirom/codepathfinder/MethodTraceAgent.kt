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
    private var config: MethodTraceRule.Config? = null
    private var isInitialized = false


    fun initialize(config: MethodTraceRule.Config) {

        if (isInitialized) {
            // Already initialized, just update config
            this.config = config
            return
        }

        this.config = config

        // Enable ByteBuddy experimental features
        System.setProperty("net.bytebuddy.experimental", "true")

        try {
            val instrumentation = net.bytebuddy.agent.ByteBuddyAgent.install()

            val agentBuilder = createAgentBuilder(config, instrumentation)

            agentBuilder.installOnByteBuddyAgent()

            isInitialized = true

        } catch (e: Exception) {
        }
    }




    @Suppress("NewApi")
    private fun createAgentBuilder(config: MethodTraceRule.Config, instrumentation: Instrumentation): net.bytebuddy.agent.builder.AgentBuilder {
        val temp = Files.createTempDirectory("tmp").toFile()
        fallbackToIndividualInjection(temp, instrumentation)
      return createAgentBuilderInstance(config)
    }

    private fun <T:Any>addClass(
      clazz: Class<T>,
        instrumentation: Instrumentation,
        temp: File
    ) {
        try{
            val classLoader = clazz.classLoader
            if (classLoader == null) {
                val codeSource = clazz.protectionDomain?.codeSource
                if (codeSource != null) {
                    try {
                        val jarFile = File(codeSource.location.toURI())
                        if (jarFile.exists() && jarFile.name.endsWith(".jar")) {
                            instrumentation.appendToBootstrapClassLoaderSearch(JarFile(jarFile))
                        }
                    } catch (e: Exception) {
                    }
                }
            } else {
                val codeSource = clazz.protectionDomain?.codeSource
                if (codeSource != null) {
                    val jarFile = File(codeSource.location.toURI())

                    instrumentation.appendToBootstrapClassLoaderSearch(JarFile(jarFile))
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun fallbackToIndividualInjection(temp: File, instrumentation: Instrumentation) {
        val classesToInject = mutableMapOf<TypeDescription.ForLoadedType, ByteArray?>()

        fun addClassAndDependencies(clazz: Class<*>) {
            try {
                classesToInject[TypeDescription.ForLoadedType(clazz)] = ClassFileLocator.ForClassLoader.read(clazz)
            } catch (e: Exception) {
            }
        }

        addClassAndDependencies(MethodTraceAdvice::class.java)
        addClassAndDependencies(MethodTraceAdvice.Companion::class.java)
        addClassAndDependencies(MethodTraceAgent::class.java)
        addClassAndDependencies(MethodTraceRule.Config::class.java)
        addClassAndDependencies(TraceEvent::class.java)
        try {
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathfinder.DefaultFilter"))
            addClassAndDependencies(Class.forName("io.github.takahirom.codepathfinder.DefaultFormatter"))
        } catch (e: Exception) {
        }
        try {
            addClassAndDependencies(Class.forName("kotlin.jvm.internal.Intrinsics"))
        } catch (e: Exception) {
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
                }
                override fun onTransformation(typeDescription: net.bytebuddy.description.type.TypeDescription, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean, dynamicType: net.bytebuddy.dynamic.DynamicType) {
                }
                override fun onIgnored(typeDescription: net.bytebuddy.description.type.TypeDescription, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean) {
                }
                override fun onError(typeName: String, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean, throwable: Throwable) {
                }
                override fun onComplete(typeName: String, classLoader: ClassLoader?, module: net.bytebuddy.utility.JavaModule?, loaded: Boolean) {
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
                return true
            }
        }
    }


}

