package com.example.screentranslator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentranslator.data.AppSettings
import com.example.screentranslator.data.SettingsRepository
import com.example.screentranslator.translate.ModelManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setTargetLang(code: String) {
        viewModelScope.launch { repository.setTargetLang(code) }
    }

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { repository.setEnabled(value) }
    }

    /** Downloads the ML Kit model for [code] so translation works offline. */
    fun downloadModel(code: String) {
        viewModelScope.launch { runCatching { ModelManager.downloadModel(code).await() } }
    }
}
