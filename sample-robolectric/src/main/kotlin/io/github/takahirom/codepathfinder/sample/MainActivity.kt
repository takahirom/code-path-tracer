package io.github.takahirom.codepathfinder.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodePathFinderTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var count by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Code Path Finder Sample",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Count: $count",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { count = calculateNewCount(count) }
        ) {
            Text("Increment")
        }
    }
}

private fun calculateNewCount(current: Int): Int {
    return current + 1
}

@Composable
fun CodePathFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CodePathFinderTheme {
        MainScreen()
    }
}