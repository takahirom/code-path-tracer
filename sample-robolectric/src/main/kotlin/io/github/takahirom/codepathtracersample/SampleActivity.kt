package io.github.takahirom.codepathtracersample

class SampleActivity {
    
    fun onCreate() {
        println("SampleActivity onCreate called")
        setupViews()
    }
    
    private fun setupViews() {
        println("Setting up views")
        val calculator = Calculator()
        val result = calculator.calculate(10, 5)
        println("Calculation result: $result")
    }
    
    class Calculator {
        fun calculate(a: Int, b: Int): Int {
            return add(a, b) * 2
        }
        
        private fun add(x: Int, y: Int): Int {
            return x + y
        }
    }
}