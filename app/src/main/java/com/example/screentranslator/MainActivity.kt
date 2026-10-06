package com.example.screentranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screentranslator.diagnostics.CrashLogger
import com.example.screentranslator.ui.settings.SettingsScreen

/**
 * Hosts the Compose settings screen: enable the accessibility service, choose the target
 * language, download ML Kit models, and toggle the overlay.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lastCrash = CrashLogger.readLastCrash(this)
        CrashLogger.clear(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (lastCrash != null) {
                        CrashScreen(lastCrash)
                    } else {
                        // If SettingsScreen itself throws during composition, show the error
                        // instead of leaving a blank window.
                        SafeSettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeSettingsScreen() {
    var crash by remember { mutableStateOf<Throwable?>(null) }
    if (crash == null) {
        runCatching { SettingsScreen() }.onFailure { crash = it }
    }
    crash?.let { CrashScreen(it.stackTraceToString()) }
}

@Composable
private fun CrashScreen(message: String) {
    Text(
        "Screen Translator hit an error on the previous launch:\n\n$message",
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    )
}
