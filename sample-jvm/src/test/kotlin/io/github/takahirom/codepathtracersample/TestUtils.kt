package io.github.takahirom.codepathtracersample

import java.io.ByteArrayOutputStream
import java.io.PrintStream

object TestUtils {
    fun captureOutput(action: () -> Unit): String {
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        
        try {
            action()
        } finally {
            System.setOut(originalOut)
        }
        
        return outputStream.toString()
    }
}