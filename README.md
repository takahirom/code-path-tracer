# code-path-finder

A ByteBuddy-based method tracing library extracted from Roborazzi. This library provides dynamic method instrumentation and tracing capabilities for JVM and Android applications.

## Features

- **Dynamic Method Tracing**: Automatically trace method calls with entry/exit logging
- **Configurable Filtering**: Include/exclude packages and methods
- **Argument & Return Value Logging**: Capture method parameters and return values
- **Nested Call Visualization**: Indent output to show call hierarchy
- **JUnit Integration**: Easy integration with JUnit tests via `MethodTraceRule`
- **Android/Robolectric Support**: Works in Android unit tests with Robolectric

## Quick Start

### Basic Usage with JUnit

```kotlin
class MyTest {
    @get:Rule
    val methodTraceRule = MethodTraceRule.builder()
        .packageIncludes("com.example.myapp")
        .packageExcludes("android", "androidx")
        .showArguments(true)
        .showReturns(true)
        .build()
    
    @Test
    fun testWithTracing() {
        val calculator = Calculator()
        val result = calculator.add(10, 5) // This will be traced
        assertEquals(15, result)
    }
}
```

### Expected Output

```
[MethodTrace] → ENTERING: Calculator.add(arg0=10, arg1=5)
[MethodTrace] ← EXITING: Calculator.add -> 15
```

## Project Structure

- `code-path-finder/`: Core library with ByteBuddy agent and tracing logic
- `sample-robolectric/`: Android library module with Robolectric test examples

## Development

This project uses [Gradle](https://gradle.org/) with multi-module setup.

### Build Commands

* Run `./gradlew build` to build all modules
* Run `./gradlew check` to run all tests
* Run `./gradlew clean` to clean build outputs
* Run `./debug-trace.sh` to run debug trace tests and see filtered output

### Debug Tracing

Use the provided debug script to quickly verify method tracing:

```bash
./debug-trace.sh
```

This script runs the test and filters output to show only the important trace logs, making it easy to verify that method instrumentation is working correctly.

### Module Testing

* Run `./gradlew sample-robolectric:testDebugUnitTest` for Android/Robolectric tests

## Technical Details

- Uses ByteBuddy for dynamic code instrumentation
- Implements Java Instrumentation API for class transformation
- Thread-safe depth tracking for nested method calls
- Configurable argument/return value length limits
- Automatic exclusion of framework and tracing-related classes

Note: This project uses the Gradle Wrapper (`./gradlew`) which is the recommended approach for production projects.