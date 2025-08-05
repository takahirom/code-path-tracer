# Code Path Tracer 🧭

<img width="1408" height="768" alt="clipboard-image-1753705301" src="https://github.com/user-attachments/assets/6adb29ac-ce64-49ec-b141-f7bf9e29c511" />

**Simple, powerful method tracing for JVM and Android(JVM Tests)**

See exactly what your code is doing with clean, visual method traces. Perfect for debugging, understanding complex codebases, and visualizing execution flow.

## 🚀 Quick Start

Just wrap your code and see what happens:

```kotlin
codePathTrace {
    calculator.complexCalculation(5, 3)
}
```

**Output:**
```
→ Calculator()
← Calculator
→ Calculator.complexCalculation(5, 3)
  → Calculator.add(5, 3)
  ← Calculator.add = 8
  → Calculator.multiply(8, 2)
  ← Calculator.multiply = 16
  → Calculator.add(16, 12)
  ← Calculator.add = 28
← Calculator.complexCalculation = 28
```

## 🎯 Android Touch Event Case Study

Ever wondered "which view actually handled my touch event?" CodePathTracer shows you:

```kotlin
@get:Rule
val traceRule = CodePathTracer.Builder()
    .filter { event -> event.methodName.contains("TouchEvent") }
    .asJUnitRule()
```

**Output reveals the touch event flow:**
```
→ PhoneWindow.superDispatchTouchEvent(MotionEvent)
  → ViewGroup.onInterceptTouchEvent(MotionEvent)
  ← ViewGroup.onInterceptTouchEvent = false
  → TextView.onTouchEvent(MotionEvent)
  ← TextView.onTouchEvent = true ✅
← PhoneWindow.superDispatchTouchEvent = true
```

**Result:** TextView handled the touch! Mystery solved. 🎯

## 💡 Motivation

Traditional debugging tools can be challenging for AI developers and complex scenarios:
- **AI limitations with debuggers** - AI assistants struggle to use breakpoints and step-through debugging effectively
- **Coverage tools miss the why** - Code coverage shows *what* was executed but not *how* the execution flowed
- **Complex debugging scenarios** - From Android touch event handling to deep method chains and callbacks, understanding execution flow is notoriously difficult

Code Path Tracer solves these problems by providing **visual execution traces** that show exactly how your code flows, making debugging accessible to both humans and AI tools.

## 📦 Installation

**Coming Soon!** 🚧

We're working on making CodePathTracer available through:
- Maven Central

For now, clone and build locally:
```bash
git clone https://github.com/takahirom/code-path-finder.git
./gradlew publishToMavenLocal
```

## ✨ Features

- 🎯 **Zero-config tracing** - Add implementation and call one method
- 🎨 **Beautiful visual output** - Clean arrows show method entry/exit with depth indentation  
- 🔧 **Flexible filtering** - Trace only what you care about

## 🎛️ Advanced Configuration

### Custom Formatting 

Want custom formatting? Configure it easily:

```kotlin
@get:Rule
val methodTraceRule = CodePathTracer.Builder()
    .filter { event -> event.className.contains("Calculator") }
    .formatter { event -> 
        when (event) {
            is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
            is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
        }
    }
    .asJUnitRule()

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

Or create a custom tracer:

```kotlin
val customTracer = CodePathTracer.Builder()
    .filter { event -> event.className.contains("Calculator") }
    .formatter { event -> 
        when (event) {
            is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
            is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
        }
    }
    .build()

codePathTrace(customTracer) {
    calculator.complexCalculation(5, 3)
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
./gradlew test  # Run all tests with tracing examples
```

This confirms that method tracing works across:
- ✅ JVM applications  
- ✅ Android applications (via Robolectric)
- ✅ Complex business logic chains

## 🛠️ Development

```bash
./gradlew build              # Build everything
./gradlew :code-path-tracer:test  # Run core tests  
./gradlew test               # Verify tracing works
```

---

*Built with ByteBuddy • Made for developers who care about understanding their code*