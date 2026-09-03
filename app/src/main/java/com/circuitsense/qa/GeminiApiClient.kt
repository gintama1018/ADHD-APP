package com.circuitsense.qa

import android.content.Context
import android.content.SharedPreferences
import com.circuitsense.model.CircuitGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct Google Gemini 1.5 Flash REST client for CircuitSense.
 * Communicates with Generative Language API using native HttpURLConnection
 * and kotlinx.serialization (zero external dependencies required).
 */
object GeminiApiClient {

    private const val PREFS_NAME = "circuitsense_ai_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    // Default public/demo endpoint fallback if user hasn't configured a key
    private var customApiKey: String? = null

    fun getApiKey(context: Context): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey!!
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setApiKey(context: Context, key: String) {
        customApiKey = key.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GEMINI_API_KEY, customApiKey).apply()
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    /**
     * Queries Google Gemini 1.5 Flash with the full live circuit context.
     */
    suspend fun queryGemini(
        prompt: String,
        graph: CircuitGraph,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API Key provided. Tap the Settings icon to enter your Google Gemini API key.")
            )
        }

        try {
            val v = graph.formula.V
            val r = graph.formula.R
            val i = graph.formula.I
            val power = v * i

            val systemInstruction = """
                You are CircuitSense AI, a friendly, world-class interactive physics professor and animation tutor.
                You specialize in explaining electricity, circuits, and Ohm's Law (V = IR) to students, visual learners, and ADHD minds.
                
                The student is currently watching an animated simulation with these EXACT values:
                - Voltage (V): $v Volts (battery)
                - Resistance (R): $r Ohms (resistor)
                - Current (I): $i Amperes (flowing in the loop)
                - Power Dissipation (P = V * I): ${String.format("%.3f", power)} Watts
                - Character: "Sparky the Electron", who is born excited at the battery terminal, glides smoothly through copper wire, and gets squished and strained through the resistor due to atomic lattice collisions (thermal friction).
                
                Guidelines:
                1. Always tailor your answer directly to the current circuit values ($v V, $r Ω, $i A).
                2. Use intuitive, vivid analogies (water pipes, crowded corridors, steep ski slopes).
                3. Keep paragraphs short, punchy, and ADHD-friendly. Use bullet points and bold key terms.
                4. Show mathematical steps cleanly when relevant.
            """.trimIndent()

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.doOutput = true

            // Build request payload
            val requestJson = buildJsonObject {
                put("contents", buildJsonArray {
                    addJsonObject {
                        put("role", "user")
                        put("parts", buildJsonArray {
                            addJsonObject {
                                put("text", "$systemInstruction\n\nStudent's Question: $prompt")
                            }
                        })
                    }
                })
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 800)
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val parsed = Json.parseToJsonElement(responseText).jsonObject
                val candidates = parsed["candidates"]?.jsonArray
                val firstCandidate = candidates?.firstOrNull()?.jsonObject
                val content = firstCandidate?.get("content")?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                val replyText = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

                if (!replyText.isNullOrBlank()) {
                    Result.success(replyText.trim())
                } else {
                    Result.failure(Exception("Empty response from Gemini API."))
                }
            } else {
                val errorStream = conn.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "HTTP Error code: $responseCode"
                }
                Result.failure(Exception("Gemini API error ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
