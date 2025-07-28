package io.github.takahirom.codepathtracersample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private var count = 0
    private lateinit var countTextView: TextView
    private val buttonId = View.generateViewId()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        
        val titleTextView = TextView(this).apply {
            text = "Code Path Tracer Sample"
            textSize = 24f
        }
        
        countTextView = TextView(this).apply {
            text = "Count: $count"
            textSize = 18f
            setPadding(0, 32, 0, 32)
        }
        
        val button = Button(this).apply {
            id = buttonId
            text = "Increment"
            setOnClickListener { handleButtonClick() }
        }
        
        layout.addView(titleTextView)
        layout.addView(countTextView)
        layout.addView(button)
        
        setContentView(layout)
    }
    
    fun handleButtonClick() {
        val processed = processClick(count)
        updateUI(processed)
    }

    fun processClick(value: Int): Int {
        return calculateValue(value)
    }

    fun calculateValue(input: Int): Int {
        return input + 1
    }
    
    fun updateUI(newCount: Int) {
        count = newCount
        countTextView.text = "Count: $count"
    }
    
    fun getButtonId(): Int = buttonId
}