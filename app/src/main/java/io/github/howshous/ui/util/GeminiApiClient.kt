package io.github.howshous.ui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object GeminiKeyProvider {
    private val apiKey: String? by lazy {
        io.github.howshous.BuildConfig.GEMINI_API_KEY
            .takeIf { it.isNotBlank() }
    }

    fun getKey(): String? = apiKey
}

object GeminiApiClient {
    private val httpClient = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchRecommendation(query: String, listingsJson: String): Result<String> = withContext(Dispatchers.IO) {
        val key = GeminiKeyProvider.getKey()
            ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))

        // Match the exact systemInstruction from the working Google AI Studio example
        val systemInstructions = """
            You are 'HowsHous AI', a friendly, expert real-estate advisor.
            Your goal is to help tenants find the best home from the provided list.
            Rules:
            1. ONLY recommend homes from the provided JSON list.
            2. Be concise but persuasive. Highlight specific features (amenities, price, etc.).
            3. If the user asks to compare, create a structured comparison of the specific listings.
            4. If the user's budget is too low for anything, suggest the closest match and explain why it's worth the stretch, or suggest the cheapest option.
            5. Use markdown for formatting (bolding key details, bullet points).
            6. If you recommend a specific listing, mention its Title and Price clearly.
            Tone: professional, warm, trustworthy.
        """.trimIndent()

        // Build prompt exactly like the working example
        val prompt = """
            User Query: "$query"
            
            Available Listings Data (JSON):
            $listingsJson
        """.trimIndent()

        // Use standard endpoint for gemini-2.5-flash
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
        
        tryRequest(endpoint, systemInstructions, prompt, useSystemInstruction = true)
    }

    private data class EndpointConfig(
        val path: String,
        val useSystemInstruction: Boolean
    )

    private fun tryRequest(
        endpoint: String,
        systemInstructions: String,
        userPrompt: String,
        useSystemInstruction: Boolean
    ): Result<String> {
        return runCatching {
            val payload = if (useSystemInstruction) {
                // For Gemini 2.0 with systemInstruction support
                JSONObject().apply {
                    put(
                        "systemInstruction",
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().apply {
                                    put(JSONObject().apply { put("text", systemInstructions) })
                                }
                            )
                        }
                    )
                    put(
                        "contents",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("role", "user")
                                    put(
                                        "parts",
                                        JSONArray().apply {
                                            put(JSONObject().apply { put("text", userPrompt) })
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.7)
                            put("topK", 40)
                            put("topP", 0.95)
                            put("maxOutputTokens", 1024)
                        }
                    )
                }.toString()
            } else {
                // For v1 API, combine system instructions with user message
                val fullPrompt = "$systemInstructions\n\n$userPrompt"
                JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put(
                                        "parts",
                                        JSONArray().apply {
                                            put(JSONObject().apply { put("text", fullPrompt) })
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.7)
                            put("topK", 40)
                            put("topP", 0.95)
                            put("maxOutputTokens", 1024)
                        }
                    )
                }.toString()
            }

            // Build request with headers to avoid location detection
            val request = Request.Builder()
                .url(endpoint)
                .post(payload.toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HowsHous-Android/1.0")
                // Remove any location-related headers that might be added by default
                .removeHeader("X-Forwarded-For")
                .removeHeader("X-Real-IP")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // Extract detailed error message from API response
                    val errorMessage = try {
                        val errorJson = JSONObject(body)
                        val errorObj = errorJson.optJSONObject("error")
                        if (errorObj != null) {
                            errorObj.optString("message") ?: errorObj.optString("status") ?: "API request failed with code ${response.code}"
                        } else {
                            // If no error object, show the full body for debugging
                            if (body.isNotEmpty()) {
                                "API request failed with code ${response.code}: $body"
                            } else {
                                "API request failed with code ${response.code}"
                            }
                        }
                    } catch (e: Exception) {
                        // If JSON parsing fails, show the raw body
                        if (body.isNotEmpty()) {
                            "API request failed with code ${response.code}: $body"
                        } else {
                            "API request failed with code ${response.code}"
                        }
                    }
                    throw IOException(errorMessage)
                }
                parseResponse(body) ?: throw IOException("Empty response from Gemini.")
            }
        }
    }

    private fun parseResponse(body: String): String? {
        return runCatching {
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.optJSONObject(0)?.optString("text")
        }.getOrNull()
    }

    // Helper function to list available models (for debugging)
    suspend fun listAvailableModels(): Result<String> = withContext(Dispatchers.IO) {
        val key = GeminiKeyProvider.getKey()
            ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))

        val endpoints = listOf(
            "https://generativelanguage.googleapis.com/v1/models?key=$key",
            "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
        )

        for (endpoint in endpoints) {
            val result = runCatching {
                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        body
                    } else {
                        null
                    }
                }
            }
            result.getOrNull()?.let { return@withContext Result.success(it) }
        }

        Result.failure(Exception("Could not list models from any endpoint"))
    }
}

