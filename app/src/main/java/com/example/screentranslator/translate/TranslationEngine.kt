package com.example.screentranslator.translate

import com.google.mlkit.nl.langid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Bridges ML Kit Language ID + Translation into a suspend API and layers the [TranslationCache]
 * on top. Translators are created per (source, target) pair and reused.
 */
class TranslationEngine(private val cache: TranslationCache) {

    private val languageIdentifier = LanguageIdentification.getClient()
    private val translators = HashMap<Pair<String, String>, Translator>()

    private fun translatorFor(source: String, target: String): Translator {
        val key = source to target
        translators[key]?.let { return it }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(ModelManager.toTranslateLanguage(source))
            .setTargetLanguage(ModelManager.toTranslateLanguage(target))
            .build()
        return Translation.getClient(options).also { translators[key] = it }
    }

    /** Detects the language of [text] (cached). Returns a BCP-47 tag, or "und" if unknown. */
    suspend fun detectLanguage(text: String): String {
        cache.getLanguage(text)?.let { return it }
        val tag = languageIdentifier.identifyLanguage(text).await()
        if (!tag.isNullOrEmpty() && tag != UNDETERMINED) {
            cache.putLanguage(text, tag)
        }
        return tag ?: UNDETERMINED
    }

    /**
     * Translates [text] into [targetTag]. Returns the original text unchanged when the source
     * can't be detected or already matches the target. Successful results are cached.
     *
     * @throws Exception if a model can't be downloaded or translation fails.
     */
    suspend fun translate(text: String, targetTag: String): String {
        cache.getTranslation(text, targetTag)?.let { return it }

        val sourceTag = detectLanguage(text)
        if (sourceTag == UNDETERMINED || sourceTag == targetTag) {
            return text
        }

        val translator = translatorFor(sourceTag, targetTag)
        translator.downloadModelIfNeeded().await()
        val result = translator.translate(text).await()
        cache.putTranslation(text, targetTag, result)
        return result
    }

    private companion object {
        const val UNDETERMINED = "und"
    }
}
