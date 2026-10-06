package com.example.screentranslator.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screentranslator.overlay.TranslateAccessibilityService
import com.example.screentranslator.translate.ModelManager
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val languages = remember {
        ModelManager.allLanguageTags().sortedBy { Locale.forLanguageTag(it).displayLanguage }
    }
    val serviceEnabled = isAccessibilityServiceEnabled(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text("Screen Translator", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Translates the text of any app into your chosen language, on-device with ML Kit.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = if (serviceEnabled) "Accessibility service: ON" else "Accessibility service: OFF",
            style = MaterialTheme.typography.titleMedium,
            color = if (serviceEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open Accessibility settings") }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Overlay translation", style = MaterialTheme.typography.titleMedium)
                Text("Show translations on screen", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = settings.enabled, onCheckedChange = viewModel::setEnabled)
        }

        Spacer(Modifier.height(20.dp))
        Text("Translate into", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LanguageDropdown(
            selected = settings.targetLang,
            languages = languages,
            onSelect = viewModel::setTargetLang,
        )

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.downloadModel(settings.targetLang) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Download language model") }

        Spacer(Modifier.height(24.dp))
        Text(
            "How to use:\n" +
                "1. Enable the accessibility service above (find \"Screen Translator\").\n" +
                "2. Open any app that shows text.\n" +
                "3. Tap the floating bubble to toggle the translation overlay.\n" +
                "4. Scroll — new text is translated as it appears.\n\n" +
                "Note: text drawn as images or canvas (games, maps, photos) has no accessible " +
                "text nodes and cannot be translated by this approach.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LanguageDropdown(
    selected: String,
    languages: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(displayName(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(displayName(tag)) },
                    onClick = {
                        onSelect(tag)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun displayName(tag: String): String {
    val name = Locale.forLanguageTag(tag).displayLanguage
    return name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, TranslateAccessibilityService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}
