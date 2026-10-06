package com.example.screentranslator.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.TranslateRemoteModelManager
import java.util.Locale

/**
 * Wraps ML Kit's remote-model manager. Each language's translation model is downloaded once
 * (needs INTERNET) and then used fully offline.
 */
object ModelManager {

    private val modelManager = TranslateRemoteModelManager.getInstance()

    /** Normalizes a BCP-47 tag to an ML Kit [TranslateLanguage] constant, defaulting to English. */
    fun toTranslateLanguage(tag: String): String =
        TranslateLanguage.fromLanguageTag(tag) ?: TranslateLanguage.ENGLISH

    /** All language tags ML Kit can translate to/from. */
    fun allLanguageTags(): Set<String> = TranslateLanguage.allLanguages()

    /** Ensures the model for [tag] is downloaded. Returns a GMS Task. */
    fun downloadModel(tag: String) = modelManager.downloadModelIfNeeded(
        TranslateRemoteModel.Builder(toTranslateLanguage(tag)).build()
    )

    /** Models currently on the device. Returns a GMS Task<Set<TranslateRemoteModel>>. */
    fun downloadedModels() = modelManager.downloadedModels

    /** Human-readable language name for a tag, e.g. "es" -> "Spanish". */
    fun displayName(tag: String): String = Locale.forLanguageTag(tag).displayLanguage
}
