package com.muso.music.utils

import android.content.Context
import android.util.LruCache
import com.muso.music.constants.AIApiKeyKey
import com.muso.music.constants.AICustomBaseURLKey
import com.muso.music.constants.AICustomModelKey
import com.muso.music.constants.AIProvider
import com.muso.music.constants.AIProviderKey
import com.muso.music.constants.AITargetLanguageKey
import com.muso.music.constants.UseAITranslationKey
import com.muso.music.db.entities.LyricsEntity
import com.muso.music.extensions.toEnum
import com.muso.music.lyrics.LyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * AI lyrics translation (SimpMusic port, adapted): when the user enables AI translation
 * and provides their own API key, lyrics are translated by the chosen provider
 * (OpenAI, Google Gemini, or any OpenAI-compatible endpoint) instead of the on-device
 * translator. The key never leaves the device except to the provider the user picked.
 *
 * Everything fails soft: on any error the original lyrics are returned untouched, so a
 * bad key, no network, or a malformed reply can never crash or blank the lyrics screen.
 */
object AITranslator {

    data class Config(
        val provider: AIProvider,
        val apiKey: String,
        val model: String,
        val baseUrl: String,
    )

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private const val MAX_CACHE_SIZE = 20
    private val cache = LruCache<String, LyricsEntity>(MAX_CACHE_SIZE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Language codes offered in the AI settings, mapped to English names for the prompt. */
    val TARGET_LANGUAGES: List<Pair<String, String>> = listOf(
        "" to "", // "system" placeholder, replaced at runtime
        "en" to "English",
        "bn" to "Bengali",
        "hi" to "Hindi",
        "ur" to "Urdu",
        "pa" to "Punjabi",
        "ta" to "Tamil",
        "te" to "Telugu",
        "mr" to "Marathi",
        "gu" to "Gujarati",
        "kn" to "Kannada",
        "ml" to "Malayalam",
        "or" to "Odia",
        "as" to "Assamese",
        "ne" to "Nepali",
        "si" to "Sinhala",
        "ar" to "Arabic",
        "fa" to "Persian",
        "id" to "Indonesian",
        "ms" to "Malay",
        "th" to "Thai",
        "vi" to "Vietnamese",
        "zh" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)",
        "ja" to "Japanese",
        "ko" to "Korean",
        "ru" to "Russian",
        "uk" to "Ukrainian",
        "es" to "Spanish",
        "pt" to "Portuguese",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "tr" to "Turkish",
        "pl" to "Polish",
    )

    /**
     * Reads the AI settings and returns the active config, or null when AI translation
     * is off, no key is set, or the custom provider has no model.
     */
    suspend fun config(context: Context): Config? {
        val prefs = context.dataStore.data.first()
        if (prefs[UseAITranslationKey] != true) return null
        val apiKey = prefs[AIApiKeyKey].orEmpty()
        if (apiKey.isEmpty()) return null
        val provider = prefs[AIProviderKey].toEnum(AIProvider.OPENAI)
        val customModel = prefs[AICustomModelKey].orEmpty()
        val model = customModel.ifEmpty { defaultModel(provider) }
        if (model.isEmpty()) return null // custom provider with no model cannot run
        val baseUrl = prefs[AICustomBaseURLKey].orEmpty().ifEmpty { "https://api.openai.com/v1/" }
        return Config(provider, apiKey, model, baseUrl)
    }

    private fun defaultModel(provider: AIProvider): String = when (provider) {
        AIProvider.OPENAI -> "gpt-4o-mini"
        AIProvider.GEMINI -> "gemini-2.0-flash"
        AIProvider.CUSTOM_OPENAI -> ""
    }

    suspend fun targetLanguage(context: Context): String {
        val prefs = context.dataStore.data.first()
        val stored = prefs[AITargetLanguageKey].orEmpty()
        return if (stored.isEmpty()) {
            // "System": translate into the device language, in English spelling for the model.
            Locale.getDefault().displayLanguage
        } else {
            TARGET_LANGUAGES.firstOrNull { it.first == stored }?.second ?: stored
        }
    }

    /**
     * Translates the lyrics with the configured provider. Returns the original entity
     * unchanged when anything goes wrong.
     */
    suspend fun translate(context: Context, lyrics: LyricsEntity): LyricsEntity {
        cache.get(lyrics.id)?.let { return it }
        val cfg = config(context) ?: return lyrics
        return runCatching {
            val isSynced = lyrics.lyrics.startsWith("[")
            val targetLanguage = targetLanguage(context)
            if (targetLanguage.isEmpty()) return lyrics

            if (isSynced) {
                val entries = LyricsUtils.parseLyrics(lyrics.lyrics)
                if (entries.isEmpty()) return lyrics
                // Numbered lines keep the model's output aligned with the timestamps.
                val numbered = entries.mapIndexed { index, entry -> "${index + 1}| ${entry.text}" }
                val translated = requestTranslation(numbered, targetLanguage, cfg)
                    ?: return lyrics
                if (translated.size != entries.size) return lyrics
                lyrics.copy(
                    lyrics = entries.mapIndexed { index, entry ->
                        "[%02d:%02d.%03d]${translated[index]}".format(
                            entry.time / 60000,
                            (entry.time / 1000) % 60,
                            entry.time % 1000,
                        )
                    }.joinToString(separator = "\n"),
                )
            } else {
                val lines = lyrics.lyrics.lines()
                if (lines.all { it.isBlank() }) return lyrics
                val numbered = lines.mapIndexed { index, line -> "${index + 1}| $line" }
                val translated = requestTranslation(numbered, targetLanguage, cfg)
                    ?: return lyrics
                if (translated.size != lines.size) return lyrics
                lyrics.copy(
                    lyrics = translated.joinToString(separator = "\n"),
                )
            }
        }.getOrDefault(lyrics).also {
            if (it !== lyrics) cache.put(it.id, it)
        }
    }

    /**
     * Sends the numbered lines in one request and returns translations in the same order,
     * or null when the reply cannot be parsed / is incomplete.
     */
    private suspend fun requestTranslation(
        numberedLines: List<String>,
        targetLanguage: String,
        cfg: Config,
    ): List<String>? = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("Translate song lyrics into ")
            append(targetLanguage)
            append(". Each input line starts with a number and a pipe, like \"12| text\". ")
            append("Reply with exactly one line per input line, keeping the same \"N| \" prefix and the same order. ")
            append("Keep blank lines blank. Output only the translated lines, no explanations, no quotes.")
            append("\n\n")
            append(numberedLines.joinToString(separator = "\n"))
        }

        val reply = when (cfg.provider) {
            AIProvider.GEMINI -> requestGemini(prompt, cfg)
            else -> requestOpenAICompatible(prompt, cfg)
        } ?: return@withContext null

        val byNumber = HashMap<Int, String>()
        reply.lines().forEach { line ->
            val match = Regex("^\\s*(\\d+)\\s*\\|\\s*(.*)$").find(line) ?: return@forEach
            val number = match.groupValues[1].toIntOrNull() ?: return@forEach
            byNumber[number] = match.groupValues[2].trim()
        }
        if (byNumber.size < numberedLines.size) return@withContext null
        (1..numberedLines.size).map { n ->
            byNumber[n] ?: return@withContext null
        }
    }

    private fun requestOpenAICompatible(prompt: String, cfg: Config): String? {
        val base = cfg.baseUrl.trimEnd('/')
        val body = JSONObject()
            .put("model", cfg.model)
            .put("temperature", 0.2)
            .put("messages", org.json.JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val request = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(body.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            return runCatching {
                JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }.getOrNull()
        }
    }

    private fun requestGemini(prompt: String, cfg: Config): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${cfg.model}:generateContent"
        val body = JSONObject()
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt))),
                ),
            )
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", cfg.apiKey)
            .post(body.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            return runCatching {
                JSONObject(text)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }.getOrNull()
        }
    }
}
