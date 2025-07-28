package io.github.takahirom.codepathtracersample

/**
 * External class with inner classes to test if timing of class loading affects tracing
 */
class ExternalClassWithInner {
    
    fun useInnerClass(): Int {
        val calculator = InnerCalculator()
        return calculator.add(10, 20)
    }
    
    fun useNestedClass(): Int {
        val processor = NestedProcessor()
        return processor.process(5)
    }
    
    // Inner class - depends on outer class instance
    inner class InnerCalculator {
        fun add(a: Int, b: Int): Int {
            println("  ExternalClassWithInner.InnerCalculator: Adding $a + $b")
            return a + b
        }
        
        fun complexCalculation(x: Int, y: Int): Int {
            println("  ExternalClassWithInner.InnerCalculator: Starting complex calculation")
            val sum = add(x, y)  // Nested call
            return sum * 2
        }
    }
    
    // Nested class - independent of outer class instance  
    class NestedProcessor {
        fun process(value: Int): Int {
            println("  ExternalClassWithInner.NestedProcessor: Processing $value")
            return value * 3
        }
        
        fun complexProcess(x: Int, y: Int): Int {
            println("  ExternalClassWithInner.NestedProcessor: Complex processing")
            val processed = process(x)
            return processed + y
        }
    }
}