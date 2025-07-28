# Code Path Tracer 🧭

<img width="1408" height="768" alt="clipboard-image-1753705301" src="https://github.com/user-attachments/assets/6adb29ac-ce64-49ec-b141-f7bf9e29c511" />

**Simple, powerful method tracing for JVM and Android(JVM Tests)**

See exactly what your code is doing with clean, visual method traces. Perfect for debugging, understanding complex codebases, and visualizing execution flow.

## 💡 Motivation

Traditional debugging tools can be challenging for AI developers and complex scenarios:
- **AI limitations with debuggers** - AI assistants struggle to use breakpoints and step-through debugging effectively
- **Coverage tools miss the why** - Code coverage shows *what* was executed but not *how* the execution flowed
- **Complex debugging scenarios** - From Android touch event handling to deep method chains and callbacks, understanding execution flow is notoriously difficult

Code Path Tracer solves these problems by providing **visual execution traces** that show exactly how your code flows, making debugging accessible to both humans and AI tools.

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

Want custom formatting? Configure it easily:

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

Or customize directly with `codePathTrace()`:

```kotlin
codePathTrace(
    CodePathTracer.Config(
        filter = { event -> event.className.contains("Calculator") },
        formatter = { event -> 
            when (event) {
                is TraceEvent.Enter -> "➤ ${event.shortClassName}.${event.methodName}(${event.args.size})"
                is TraceEvent.Exit -> "⬅ ${event.shortClassName}.${event.methodName} = ${event.returnValue}"
            }
        }
    )
) {
    calculator.complexCalculation(5, 3)
}
```

## ✨ Features

- 🎯 **Zero-config tracing** - Add implementation and call one method
- 🎨 **Beautiful visual output** - Clean arrows show method entry/exit with depth indentation  
- 🔧 **Flexible filtering** - Trace only what you care about

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