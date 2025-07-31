# Proposal: Unify API Interface

## Problem Statement

Currently, Code Path Tracer provides multiple inconsistent APIs for configuration:

1. **Builder Pattern** (CodePathTracerRule.builder())
2. **Config Object** (CodePathTracer.Config)  
3. **DSL-style Functions** (codePathTrace with lambda)

This fragmentation creates confusion and inconsistent user experience:

```kotlin
// Approach 1: Builder Pattern (JUnit Rule)
@get:Rule
val traceRule = CodePathTracerRule.builder()
    .filter { event -> event.className.contains("MyClass") }
    .formatter { event -> "..." }
    .build()

// Approach 2: Config Object
val config = CodePathTracer.Config(
    filter = { event -> event.className.contains("MyClass") },
    formatter = { event -> "..." }
)
codePathTrace(config) { /* code */ }

// Approach 3: DSL-style
codePathTrace({
    filter { event -> event.className.contains("MyClass") }
    formatter { event -> "..." }
}) { /* code */ }
```

## Proposed Solution

### Unified Builder API

Based on lessons learned from libraries like Coil, OkHttp, and Retrofit, replace all three approaches with a single, consistent Builder-based API:

```kotlin
// Simple usage with default settings
codePathTrace {
    myObject.complexMethod()
}

// Custom configuration with Builder
val customTracer = CodePathTracer.Builder()
    .filter { event -> event.className.contains("MyClass") }
    .formatter { event -> "Custom: ${event.methodName}" }
    .maxDepth(5)
    .enabled(true)
    .build()

// Use custom tracer
codePathTrace(customTracer) {
    myObject.complexMethod()
}

// Direct builder usage (also supported)
customTracer.trace {
    myObject.complexMethod()
}

// JUnit Rule integration
@get:Rule
val traceRule = CodePathTracer.Builder()
    .filter { event -> event.className.contains("MyClass") }
    .formatter { event -> "Custom: ${event.methodName}" }
    .asJUnitRule()
```

### Benefits

Based on Coil's experience and industry best practices:

1. **Improved IDE Support**: Smaller scope means faster, more accurate autocomplete
2. **Clear Separation**: Configuration vs execution phases are explicitly separated
3. **Type Safety**: Builder pattern allows progressive type refinement through method chaining
4. **Java Compatibility**: Fully compatible with Java consumers
5. **Factory Pattern**: Supports instance reuse and `newBuilder()` style copying
6. **Industry Standard**: Consistent with OkHttp, Retrofit, and other major libraries
7. **Simple Default Usage**: `codePathTrace { }` works with no configuration needed
8. **Flexible Integration**: Can use default tracer or custom tracer with same API

## Implementation Plan

### Phase 1: Implement Builder Pattern

Create new unified Builder API while maintaining backward compatibility:

```kotlin
// New primary API - following Coil's approach
class CodePathTracer private constructor(
    private val filter: (TraceEvent) -> Boolean,
    private val formatter: (TraceEvent) -> String,
    private val enabled: Boolean,
    private val maxDepth: Int
) {
    
    class Builder {
        private var filter: (TraceEvent) -> Boolean = DefaultFilter::filter
        private var formatter: (TraceEvent) -> String = DefaultFormatter::format
        private var enabled: Boolean = true
        private var maxDepth: Int = Int.MAX_VALUE
        
        fun filter(predicate: (TraceEvent) -> Boolean) = apply { this.filter = predicate }
        fun formatter(fn: (TraceEvent) -> String) = apply { this.formatter = fn }
        fun enabled(value: Boolean) = apply { this.enabled = value }
        fun maxDepth(depth: Int) = apply { this.maxDepth = depth }
        
        fun build() = CodePathTracer(filter, formatter, enabled, maxDepth)
        
        fun asJUnitRule() = CodePathTracerRule(build())
    }
    
    fun <T> trace(block: () -> T): T {
        // Implementation
    }
    
    fun newBuilder(): Builder {
        return Builder()
            .filter(filter)
            .formatter(formatter)
            .enabled(enabled)
            .maxDepth(maxDepth)
    }
    
    companion object {
        fun Builder() = Builder()
    }
}

// Default tracer instance
private val DEFAULT_TRACER = CodePathTracer.Builder().build()

// Primary API - simple usage with default settings
fun <T> codePathTrace(block: () -> T): T = DEFAULT_TRACER.trace(block)

// Primary API - usage with custom tracer
fun <T> codePathTrace(tracer: CodePathTracer, block: () -> T): T = tracer.trace(block)

// Backward compatibility function (deprecated)
@Deprecated("Use CodePathTracer.Builder() instead")
fun <T> codePathTrace(
    filter: (TraceEvent) -> Boolean = DefaultFilter::filter,
    formatter: (TraceEvent) -> String = DefaultFormatter::format,
    enabled: Boolean = true,
    block: () -> T
): T {
    return CodePathTracer.Builder()
        .filter(filter)
        .formatter(formatter)
        .enabled(enabled)
        .build()
        .trace(block)
}
```

### Phase 2: Instance Reuse and Sharing

Following OkHttp's pattern for instance sharing:

```kotlin
// Shared configuration example
val baseTracer = CodePathTracer.Builder()
    .formatter { event -> "Base: ${event.methodName}" }
    .enabled(true)
    .build()

// Create variations using newBuilder()
val testTracer = baseTracer.newBuilder()
    .filter { event -> event.className.contains("Test") }
    .build()

val productionTracer = baseTracer.newBuilder()
    .filter { event -> !event.className.contains("Debug") }
    .maxDepth(3)
    .build()
```

### Phase 3: Deprecation Path

1. Mark old APIs as `@Deprecated` with migration hints
2. Update documentation and samples to use new API
3. Provide automated migration tools if possible
4. Remove deprecated APIs in next major version

## Examples

### Basic Usage

```kotlin
// Simple tracing with default settings
codePathTrace {
    calculator.complexCalculation(5, 3)
}

// With custom configuration
val configuredTracer = CodePathTracer.Builder()
    .filter { event -> !event.methodName.startsWith("get") }
    .formatter { event -> "➤ ${event.shortClassName}.${event.methodName}" }
    .build()
    
codePathTrace(configuredTracer) {
    userService.processUser(user)
}
```

### Advanced Configuration

```kotlin
val advancedTracer = CodePathTracer.Builder()
    .filter { event -> 
        event.className.startsWith("com.myapp") && 
        event.depth < 5
    }
    .formatter { event ->
        when (event) {
            is TraceEvent.Enter -> "→ ${event.fullMethodName}(${event.args.size})"
            is TraceEvent.Exit -> "← ${event.fullMethodName} = ${event.returnValue}"
        }
    }
    .maxDepth(10)
    .enabled(System.getProperty("tracing.enabled") == "true")
    .build()

codePathTrace(advancedTracer) {
    // Complex business logic here
    businessLogic.processWorkflow()
}
```

### Testing Integration

```kotlin
class MyTest {
    @get:Rule
    val tracer = CodePathTracer.Builder()
        .filter { event -> event.className.contains("MyService") }
        .asJUnitRule()
    
    @Test
    fun testBusinessLogic() {
        // Test code - automatically traced
        myService.doSomething()
    }
}
```

## Migration Strategy

### For Library Users

Old code continues to work during transition period:

```kotlin
// Old API (still works, but deprecated)
@Deprecated("Use CodePathTracer.Builder().asJUnitRule() instead")
val rule = CodePathTracerRule.builder()
    .filter { ... }
    .build()

// New API
val rule = CodePathTracer.Builder()
    .filter { ... }
    .asJUnitRule()
```

### Documentation Updates

- Update README.md with new primary examples
- Add migration guide with before/after comparisons
- Create video/blog post explaining benefits

## Conclusion

This unified API will provide:

- **Better developer experience** through consistent, discoverable interface
- **Reduced cognitive load** by eliminating multiple configuration patterns  
- **Future-proof design** that can accommodate new features seamlessly
- **Smooth migration path** that doesn't break existing code

The Builder approach aligns with proven Kotlin library design patterns used by industry leaders like Google, Square, and Facebook, providing a stable foundation for both simple and advanced use cases while ensuring optimal IDE support and maintainability.