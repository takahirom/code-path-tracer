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

### Unified DSL API

Replace all three approaches with a single, consistent DSL-based API:

```kotlin
// Primary API: DSL-based configuration
codePathTrace {
    filter { event -> event.className.contains("MyClass") }
    formatter { event -> "Custom: ${event.methodName}" }
    maxDepth(5)
    enabled(true)
    
    // Execute traced code
    execute {
        myObject.complexMethod()
    }
}

// Alternative: Traditional callback style (for compatibility)
codePathTrace(
    filter = { event -> event.className.contains("MyClass") },
    formatter = { event -> "Custom: ${event.methodName}" }
) {
    myObject.complexMethod()
}

// JUnit Rule integration
@get:Rule
val traceRule = codePathTrace.asJUnitRule {
    filter { event -> event.className.contains("MyClass") }
    formatter { event -> "Custom: ${event.methodName}" }
}
```

### Benefits

1. **Consistency**: Single API style across all use cases
2. **Discoverability**: IDE auto-completion works better with DSL
3. **Readability**: Configuration and execution are clearly separated
4. **Extensibility**: Easy to add new configuration options
5. **Type Safety**: Kotlin DSL provides compile-time checking

## Implementation Plan

### Phase 1: Introduce Unified DSL

Create new unified API while maintaining backward compatibility:

```kotlin
// New primary API
class CodePathTraceBuilder {
    private var filter: (TraceEvent) -> Boolean = DefaultFilter::filter
    private var formatter: (TraceEvent) -> String = DefaultFormatter::format
    private var enabled: Boolean = true
    private var maxDepth: Int = Int.MAX_VALUE
    
    fun filter(predicate: (TraceEvent) -> Boolean) { this.filter = predicate }
    fun formatter(fn: (TraceEvent) -> String) { this.formatter = fn }
    fun enabled(value: Boolean) { this.enabled = value }
    fun maxDepth(depth: Int) { this.maxDepth = depth }
    
    fun <T> execute(block: () -> T): T {
        // Implementation
    }
    
    internal fun toConfig() = CodePathTracer.Config(filter, formatter, enabled, ...)
}

// Top-level function
fun codePathTrace(configure: CodePathTraceBuilder.() -> Unit): CodePathTraceBuilder {
    return CodePathTraceBuilder().apply(configure)
}

// Overload for backward compatibility
fun <T> codePathTrace(
    filter: (TraceEvent) -> Boolean = DefaultFilter::filter,
    formatter: (TraceEvent) -> String = DefaultFormatter::format,
    enabled: Boolean = true,
    block: () -> T
): T {
    return codePathTrace {
        filter(filter)
        formatter(formatter)
        enabled(enabled)
        execute(block)
    }
}
```

### Phase 2: JUnit Rule Integration

```kotlin
// Extension function for JUnit integration
fun CodePathTraceBuilder.asJUnitRule(): CodePathTracerRule {
    return CodePathTracerRule(this.toConfig())
}

// Usage
@get:Rule
val traceRule = codePathTrace {
    filter { event -> event.className.contains("MyClass") }
}.asJUnitRule()
```

### Phase 3: Deprecation Path

1. Mark old APIs as `@Deprecated` with migration hints
2. Update documentation and samples to use new API
3. Provide automated migration tools if possible
4. Remove deprecated APIs in next major version

## Examples

### Basic Usage

```kotlin
// Simple tracing (current: codePathTrace { })
codePathTrace {
    execute {
        calculator.complexCalculation(5, 3)
    }
}

// With configuration
codePathTrace {
    filter { event -> !event.methodName.startsWith("get") }
    formatter { event -> "➤ ${event.shortClassName}.${event.methodName}" }
    
    execute {
        userService.processUser(user)
    }
}
```

### Advanced Configuration

```kotlin
codePathTrace {
    filter { event -> 
        event.className.startsWith("com.myapp") && 
        event.depth < 5
    }
    
    formatter { event ->
        when (event) {
            is TraceEvent.Enter -> "→ ${event.fullMethodName}(${event.args.size})"
            is TraceEvent.Exit -> "← ${event.fullMethodName} = ${event.returnValue}"
        }
    }
    
    maxDepth(10)
    enabled(System.getProperty("tracing.enabled") == "true")
    
    execute {
        // Complex business logic here
        businessLogic.processWorkflow()
    }
}
```

### Testing Integration

```kotlin
class MyTest {
    @get:Rule
    val tracer = codePathTrace {
        filter { event -> event.className.contains("MyService") }
    }.asJUnitRule()
    
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
@Deprecated("Use codePathTrace { }.asJUnitRule() instead")
val rule = CodePathTracerRule.builder()
    .filter { ... }
    .build()

// New API
val rule = codePathTrace {
    filter { ... }
}.asJUnitRule()
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

The DSL approach aligns with modern Kotlin library design patterns and provides the flexibility needed for both simple and advanced use cases.