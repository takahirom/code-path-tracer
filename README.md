# Code Path Tracer 🔍

**Simple, powerful method tracing for JVM and Android**

See exactly what your code is doing with clean, visual method traces. Perfect for debugging, understanding complex codebases, and visualizing execution flow.

## ✨ Features

- 🎯 **Zero-config tracing** - Works out of the box with JUnit
- 🎨 **Beautiful output** - Visual arrows show method entry/exit 
- 🔧 **Flexible filtering** - Trace only what you care about
- 📱 **Android support** - Works with Robolectric tests
- ⚡ **Lightweight** - Minimal overhead, maximum insight

## 🚀 Quick Start

Just wrap your code and see what happens:

```kotlin
codePathTrace {
    calculator.complexCalculation(5, 3)
}
```

**Output:**
```
→ JvmMethodTraceTest$testSimpleCodePathTrace$1.invoke()
← JvmMethodTraceTest$testSimpleCodePathTrace$1.invoke = 28
```

Want beautiful formatting? Easy!

```kotlin
codePathTrace({ 
    filter { it.className.contains("Calculator") }
    formatter { event ->
        when (event) {
            is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
            is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
        }
    }
}) {
    calculator.complexCalculation(5, 3)
}
```

**Output:**
```
➤ Calculator.complexCalculation(2)
➤ Calculator.add(2)
⬅ Calculator.add = 8
➤ Calculator.multiply(2) 
⬅ Calculator.multiply = 16
➤ Calculator.add(2)
⬅ Calculator.add = 28
⬅ Calculator.complexCalculation = 28
```

## 🎨 Custom Formatting

Want different symbols? Easy!

```kotlin
@get:Rule
val tracer = CodePathTracerRule.builder()
    .filter { event -> event.className.contains("Calculator") }
    .formatter { event -> 
        when (event) {
            is TraceEvent.Enter -> "➤ ${event.fullMethodName}(${event.args.size})"
            is TraceEvent.Exit -> "⬅ ${event.fullMethodName} = ${event.returnValue}"
        }
    }
    .build()
```

## 🎛️ Advanced Usage

### JUnit Rule Integration

For test automation, use the JUnit Rule:

```kotlin
class MyTest {
    @get:Rule
    val tracer = CodePathTracerRule.builder()
        .filter { event -> event.className.contains("Calculator") }
        .build()
    
    @Test 
    fun testCalculator() {
        calculator.add(10, 5)  // ← Automatically traced!
    }
}
```

### Filtering Examples

```kotlin
// Trace only your app code
.filter { event -> event.className.startsWith("com.mycompany") }

// Skip common methods
.filter { event -> 
    !listOf("toString", "hashCode", "equals").contains(event.methodName)
}

// Focus on specific depth levels
.filter { event -> event.depth < 3 }
```

## 🏃‍♂️ Quick Verification

Verify everything works:

```bash
./debug-trace.sh
```

This checks that method tracing works across:
- ✅ Project code (MainActivity.onCreate)  
- ✅ Library code (SnapshotThreadLocal.get)
- ✅ Android Framework (PhoneWindow.getPanelState)

## 🛠️ Development

```bash
./gradlew build              # Build everything
./gradlew :code-path-tracer:test  # Run core tests  
./debug-trace.sh             # Verify tracing works
```

---

*Built with ByteBuddy • Made for developers who care about understanding their code*