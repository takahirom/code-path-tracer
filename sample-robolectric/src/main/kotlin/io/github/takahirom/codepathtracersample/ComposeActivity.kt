package io.github.takahirom.codepathtracersample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CounterScreen()
            }
        }
    }
}

@Composable
fun CounterScreen() {
    var count by remember { mutableStateOf(0) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Code Path Tracer Sample",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            CountDisplay(count = count)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CounterButton(
                onClick = { 
                    count = calculateNewValue(count)
                }
            )
        }
    }
}

@Composable
fun CountDisplay(count: Int) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .semantics { contentDescription = "Count display showing $count" },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = "Count: $count",
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CountDisplayPreview() {
    MaterialTheme {
        CountDisplay(count = 42)
    }
}

@Composable
fun CounterButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .semantics { contentDescription = "Increment counter button" }
    ) {
        Text(
            text = "Increment",
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterButtonPreview() {
    MaterialTheme {
        CounterButton(onClick = {})
    }
}

fun calculateNewValue(value: Int): Int {
    return value + 1
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    MaterialTheme {
        CounterScreen()
    }
}