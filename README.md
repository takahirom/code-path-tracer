# Code Path Tracer 🔍

**Simple, powerful method tracing for JVM and Android**

See exactly what your code is doing with clean, visual method traces. Perfect for debugging, understanding complex codebases, and visualizing execution flow.

## ✨ Features

- 🎯 **Zero-config tracing** - Works out of the box with JUnit
- 🎨 **Beautiful output** - Visual arrows show method entry/exit with depth indentation
- 🔧 **Flexible filtering** - Trace only what you care about
- 📱 **Android support** - Works with Robolectric tests
- 🏗️ **Constructor tracing** - See object creation with arguments
- 🔄 **Inner class support** - Automatic retransformation for inner classes
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
               → JvmMethodTraceTest$SampleCalculator()
               ← JvmMethodTraceTest$SampleCalculator = null
               → JvmMethodTraceTest$testSimpleCodePathTrace$1.invoke()
               → JvmMethodTraceTest$SampleCalculator.complexCalculation(5, 3)
               → JvmMethodTraceTest$SampleCalculator.add(5, 3)
               ← JvmMethodTraceTest$SampleCalculator.add = 8
               → JvmMethodTraceTest$SampleCalculator.multiply(8, 2)
               ← JvmMethodTraceTest$SampleCalculator.multiply = 16
               → JvmMethodTraceTest$SampleCalculator.add(16, 12)
               ← JvmMethodTraceTest$SampleCalculator.add = 28
               ← JvmMethodTraceTest$SampleCalculator.complexCalculation = 28
               ← JvmMethodTraceTest$testSimpleCodePathTrace$1.invoke = 28
```

Want custom formatting? Use JUnit Rules:

```kotlin
@get:Rule
val methodTraceRule = CodePathTracerRule.builder()
    .filter { event -> event.className.contains("Calculator") }
    .formatter { event -> 
        when (event) {
            is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
            is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
        }
    }
    .build()

@Test
fun testCalculator() {
    calculator.complexCalculation(5, 3)
}
```

**Output:**
```
➤ SampleCalculator.complexCalculation(2)
➤ SampleCalculator.add(2)
⬅ SampleCalculator.add = 8
➤ SampleCalculator.multiply(2) 
⬅ SampleCalculator.multiply = 16
➤ SampleCalculator.add(2)
⬅ SampleCalculator.add = 28
⬅ SampleCalculator.complexCalculation = 28
```

## 🎛️ Advanced Configuration

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

### Constructor Tracing

See object creation in action:

```kotlin
class Calculator(private val name: String = "DefaultCalculator") {
    init {
        println("Initializing $name")
    }
}

val calc = Calculator("MyCalculator")  // ← Traced automatically!
```

**Output:**
```
➤ Calculator(1)
  Initializing MyCalculator
⬅ Calculator = null
```

### Inner Class Support

Inner classes are automatically detected and traced:

```kotlin
class OuterClass {
    inner class InnerCalculator {
        fun add(a: Int, b: Int) = a + b
    }
}

val calc = OuterClass().InnerCalculator()
calc.add(5, 3)  // ← Inner class methods traced!
```

**Configuration Options:**

```kotlin
val config = CodePathTracer.Config(
    autoRetransform = true,  // Enable inner class tracing (default: true)
    filter = { event -> event.className.contains("MyClass") },
    formatter = TraceEvent::defaultFormat
)
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