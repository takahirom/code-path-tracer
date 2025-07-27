package io.github.takahirom.codepathfinder

import java.util.Vector

/**
 * ByteBuddy自動変換エージェント（設定可能版）
 */
object MethodTraceAgent {
    private const val DEBUG = true
    private var config: MethodTraceRule.Config? = null
    private var isInitialized = false
    
    // Static initialization block
    init {
        try {
            val defaultConfig = MethodTraceRule.Config()
            initialize(defaultConfig)
        } catch (e: Exception) {
            // Silent initialization
        }
    }
    
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
            val instrumentation = Class.forName("net.bytebuddy.agent.ByteBuddyAgent").getMethod("install").invoke(null)
            if (DEBUG) println("[MethodTrace] ByteBuddy agent installed: $instrumentation")
            
            val transformer = createTransformer(config)
            if (DEBUG) println("[MethodTrace] Created transformer: $transformer")
            
            Class.forName("java.lang.instrument.Instrumentation")
                .getMethod("addTransformer", Class.forName("java.lang.instrument.ClassFileTransformer"), Boolean::class.javaPrimitiveType)
                .invoke(instrumentation, transformer, true)
            if (DEBUG) println("[MethodTrace] Transformer added to instrumentation")
                
            // retransformClasses強制実行
            retransformExistingClasses(instrumentation!!)
            
            isInitialized = true
            if (DEBUG) println("[MethodTrace] Agent initialization completed successfully")
                
        } catch (e: Exception) {
            if (DEBUG) {
                println("[MethodTrace] Agent initialization FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun retransformExistingClasses(instrumentation: Any) {
        try {
            val instClass = Class.forName("java.lang.instrument.Instrumentation")
            
            val canRetransform = instClass.getMethod("isRetransformClassesSupported").invoke(instrumentation) as Boolean
            if (DEBUG) println("[MethodTrace] Retransform supported: $canRetransform")
            
            if (canRetransform) {
                // 対象クラスを動的に探す場合はここに実装
                // 汎用化版では特定のクラスを指定しない
                if (DEBUG) println("[MethodTrace] Retransform setup completed")
            }
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Retransform failed: ${e.message}")
        }
    }
    
    private fun createTransformer(config: MethodTraceRule.Config): Any {
        if (DEBUG) println("[MethodTrace] createTransformer called with config: $config")
        
        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            if (DEBUG) println("[MethodTrace] Transformer invoked: method=${method.name}, args.size=${args?.size}")
            
            if (method.name == "transform") {
                if (args?.size == 6) {
                    val loader = args[0] as? ClassLoader
                    val moduleName = args[1] as? String
                    val className = args[2] as? String
                    val classBeingRedefined = args[3] as? Class<*>
                    val protectionDomain = args[4]
                    val classfileBuffer = args[5] as? ByteArray
                    
                    if (DEBUG) println("[MethodTrace] Transform called for: className=$className, loader=$loader, module=$moduleName, redefined=$classBeingRedefined, bufferSize=${classfileBuffer?.size}")
                    
                    val currentConfig = this@MethodTraceAgent.config ?: config
                    if (shouldTransformClass(className, currentConfig)) {
                        if (DEBUG) println("[MethodTrace] Transform approved for: $className")
                        
                        try {
                            val result = if (classBeingRedefined != null && classfileBuffer != null) {
                                // Simple retransformClasses case
                                val transformedBytes = net.bytebuddy.ByteBuddy()
                                    .redefine(classBeingRedefined, net.bytebuddy.dynamic.ClassFileLocator.Simple.of(classBeingRedefined.name, classfileBuffer))
                                    .visit(net.bytebuddy.asm.Advice.to(MethodTraceAdvice::class.java).on(
                                        net.bytebuddy.matcher.ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                                            .and(net.bytebuddy.matcher.ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isConstructor()))
                                            .and(net.bytebuddy.matcher.ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isTypeInitializer()))
                                    ))
                                    .make()
                                    .bytes
                                
                                if (DEBUG) println("[MethodTrace] Successfully transformed class: $className with ${transformedBytes.size} bytes")
                                transformedBytes
                            } else if (classfileBuffer != null && className != null) {
                                // Cross-module class loading case
                                val classFileLocator = net.bytebuddy.dynamic.ClassFileLocator.Compound(
                                    net.bytebuddy.dynamic.ClassFileLocator.Simple.of(className.replace("/", "."), classfileBuffer),
                                    net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader.ofSystemLoader(),
                                    net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader.of(Thread.currentThread().contextClassLoader),
                                    *getAllAvailableClassFileLocators().toTypedArray()
                                )
                                val typePool = net.bytebuddy.pool.TypePool.Default.of(classFileLocator)
                                val typeDescription = typePool.describe(className.replace("/", ".")).resolve()
                                
                                val transformedBytes = net.bytebuddy.ByteBuddy()
                                    .redefine<Any>(typeDescription, classFileLocator)
                                    .visit(net.bytebuddy.asm.Advice.to(MethodTraceAdvice::class.java).on(
                                        net.bytebuddy.matcher.ElementMatchers.any<net.bytebuddy.description.method.MethodDescription>()
                                            .and(net.bytebuddy.matcher.ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isConstructor()))
                                            .and(net.bytebuddy.matcher.ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isTypeInitializer()))
                                    ))
                                    .make()
                                    .bytes
                                
                                if (DEBUG) println("[MethodTrace] Successfully transformed class (new-load): $className with ${transformedBytes.size} bytes")
                                transformedBytes
                            } else {
                                null
                            }
                            
                            if (result != null) {
                                return@InvocationHandler result
                            }
                        } catch (e: Exception) {
                            if (DEBUG) {
                                println("[MethodTrace] Transform FAILED for $className: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    } else {
                        if (DEBUG) println("[MethodTrace] Transform REJECTED for: $className")
                    }
                } else {
                    if (DEBUG) println("[MethodTrace] Transform called with unexpected args.size: ${args?.size}")
                }
            } else {
                if (DEBUG) println("[MethodTrace] Transformer method called: ${method.name}")
            }
            null
        }
        
        return java.lang.reflect.Proxy.newProxyInstance(
            Class.forName("java.lang.instrument.ClassFileTransformer").classLoader,
            arrayOf(Class.forName("java.lang.instrument.ClassFileTransformer")),
            handler
        )
    }
    
    fun getConfig(): MethodTraceRule.Config? = config
    
    private fun getAllAvailableClassFileLocators(): List<net.bytebuddy.dynamic.ClassFileLocator> {
        val locators = mutableListOf<net.bytebuddy.dynamic.ClassFileLocator>()
        
        try {
            // Add ClassFileLocators for all available ClassLoaders
            val allClassLoaders = mutableSetOf<ClassLoader>()
            
            // System ClassLoader
            allClassLoaders.add(ClassLoader.getSystemClassLoader())
            
            // Current thread ClassLoader
            Thread.currentThread().contextClassLoader?.let { allClassLoaders.add(it) }
            
            // Application ClassLoader
            try {
                val appClassLoader = this::class.java.classLoader
                if (appClassLoader != null) {
                    allClassLoaders.add(appClassLoader)
                }
            } catch (_: Exception) {
                // Ignore
            }
            
            // Create ClassFileLocators for each unique ClassLoader
            allClassLoaders.forEach { classLoader ->
                try {
                    locators.add(net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader.of(classLoader))
                } catch (_: Exception) {
                    // Continue with next ClassLoader
                }
            }
            
        } catch (e: Exception) {
            if (DEBUG) println("[MethodTrace] Failed to create additional ClassFileLocators: ${e.message}")
        }
        
        return locators
    }
    
    private fun shouldTransformClass(className: String?, config: MethodTraceRule.Config): Boolean {
        if (className == null) {
            if (DEBUG) println("[MethodTrace] shouldTransformClass: className is null")
            return false
        }
        
        val classPath = className.replace("/", ".")
        if (DEBUG) println("[MethodTrace] shouldTransformClass: evaluating $className (classPath: $classPath) with config.packageIncludes=${config.packageIncludes}")
        
        // Package exclusion check
        if (config.packageExcludes.any { classPath.contains(it) }) {
            if (DEBUG) println("[MethodTrace] shouldTransformClass: EXCLUDED by packageExcludes - $className")
            return false
        }
        
        // Exclude tracing-related classes (only the actual MethodTrace infrastructure classes)
        if (className.contains("MethodTrace") && classPath.contains("codepathfinder") && !classPath.contains("sample")) {
            if (DEBUG) println("[MethodTrace] shouldTransformClass: EXCLUDED as MethodTrace class - $className")
            return false
        }
        
        // Package inclusion check
        val shouldInclude = config.packageIncludes.any { 
            val pattern = it.replace(".", "/")
            val matches = className.contains(pattern)
            if (DEBUG) println("[MethodTrace] shouldTransformClass: checking pattern '$pattern' against '$className' -> $matches")
            matches
        }
        
        if (DEBUG) println("[MethodTrace] shouldTransformClass: FINAL DECISION for $className -> $shouldInclude")
        return shouldInclude
    }
}