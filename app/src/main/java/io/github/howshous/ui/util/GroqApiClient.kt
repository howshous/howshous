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

object GroqKeyProvider {
    private val apiKey: String? by lazy {
        val key = io.github.howshous.BuildConfig.GROQ_API_KEY
        if (key.isBlank()) {
            android.util.Log.w("GroqKeyProvider", "GROQ_API_KEY is empty or not set in local.properties")
            null
        } else {
            android.util.Log.d("GroqKeyProvider", "GROQ_API_KEY found (length: ${key.length})")
            key
        }
    }

    fun getKey(): String? = apiKey
}

object GroqApiClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun fetchRecommendation(query: String, listingsJson: String): Result<String> = withContext(Dispatchers.IO) {
        val key = GroqKeyProvider.getKey()
            ?: return@withContext Result.failure(IllegalStateException("Groq API key is not configured. Please add GROQ_API_KEY to local.properties"))

        android.util.Log.d("GroqApiClient", "Making request to Groq API with model: llama-3.1-8b-instant")

        val systemMessage = """
            You are 'HowsHous AI', a friendly, expert real-estate advisor.
            Your goal is to help tenants find the best home from the provided list.
            Rules:
            1. ONLY recommend homes from the provided JSON list.
            2. Be concise but persuasive. Highlight specific features (amenities, price, etc.).
            3. If the user asks to compare, create a structured comparison of the specific listings.
            4. If the user's budget is too low for anything, suggest the closest match and explain why it's worth the stretch, or suggest the cheapest option.
            5. Use markdown for formatting (bolding key details, bullet points).
            6. If you recommend a specific listing, mention its Title and Price clearly.
            7. Be conversational and adapt to the user's tone and questions naturally.
            Tone: professional, warm, trustworthy.
        """.trimIndent()

        val userPrompt = """
            User Query: "$query"
            
            Available Listings Data (JSON):
            $listingsJson
        """.trimIndent()

        return@withContext try {
            val payload = JSONObject().apply {
                // Using llama-3.1-8b-instant (free tier model on Groq)
                // Alternative models: llama-3.1-70b-versatile, mixtral-8x7b-32768, gemma-7b-it
                put("model", "llama-3.1-8b-instant")
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemMessage)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        })
                    }
                )
                put("temperature", 0.7)
                put("max_tokens", 1000)
            }.toString()

            android.util.Log.d("GroqApiClient", "Request payload created, making HTTP request...")

            val request = Request.Builder()
                .url(endpoint)
                .post(payload.toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $key")
                .header("User-Agent", "HowsHous-Android/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                android.util.Log.d("GroqApiClient", "Response code: ${response.code}, body length: ${body.length}")
                
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val errorJson = JSONObject(body)
                        val errorObj = errorJson.optJSONObject("error")
                        if (errorObj != null) {
                            val message = errorObj.optString("message", "")
                            val type = errorObj.optString("type", "")
                            android.util.Log.e("GroqApiClient", "API Error - Type: $type, Message: $message")
                            message.ifEmpty { type.ifEmpty { "API request failed with code ${response.code}" } }
                        } else {
                            if (body.isNotEmpty()) {
                                android.util.Log.e("GroqApiClient", "API Error Response: $body")
                                "API request failed with code ${response.code}: $body"
                            } else {
                                "API request failed with code ${response.code}"
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GroqApiClient", "Error parsing error response: ${e.message}", e)
                        if (body.isNotEmpty()) {
                            "API request failed with code ${response.code}: $body"
                        } else {
                            "API request failed with code ${response.code}"
                        }
                    }
                    Result.failure(IOException(errorMessage))
                } else {
                    val parsed = parseResponse(body)
                    if (parsed != null) {
                        android.util.Log.d("GroqApiClient", "Successfully parsed response (length: ${parsed.length})")
                        Result.success(parsed)
                    } else {
                        android.util.Log.e("GroqApiClient", "Failed to parse response. Body: $body")
                        Result.failure(IOException("Empty or invalid response from Groq API. Response: $body"))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GroqApiClient", "Exception during API call: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseResponse(body: String): String? {
        return runCatching {
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
            message.optString("content")
        }.getOrNull()
    }
}

