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

object OpenAIKeyProvider {
    private val apiKey: String? by lazy {
        io.github.howshous.BuildConfig.OPENAI_API_KEY
            .takeIf { it.isNotBlank() }
    }

    fun getKey(): String? = apiKey
}

object OpenAIApiClient {
    private val httpClient = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://api.openai.com/v1/chat/completions"

    suspend fun fetchRecommendation(query: String, listingsJson: String): Result<String> = withContext(Dispatchers.IO) {
        val key = OpenAIKeyProvider.getKey()
            ?: return@withContext Result.failure(IllegalStateException("OpenAI API key is not configured."))

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
                put("model", "gpt-4o-mini") // Using gpt-4o-mini for cost-effectiveness
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

            val request = Request.Builder()
                .url(endpoint)
                .post(payload.toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $key")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val errorJson = JSONObject(body)
                        val errorObj = errorJson.optJSONObject("error")
                        if (errorObj != null) {
                            errorObj.optString("message") ?: errorObj.optString("type") ?: "API request failed with code ${response.code}"
                        } else {
                            if (body.isNotEmpty()) {
                                "API request failed with code ${response.code}: $body"
                            } else {
                                "API request failed with code ${response.code}"
                            }
                        }
                    } catch (e: Exception) {
                        if (body.isNotEmpty()) {
                            "API request failed with code ${response.code}: $body"
                        } else {
                            "API request failed with code ${response.code}"
                        }
                    }
                    Result.failure(IOException(errorMessage))
                } else {
                    parseResponse(body)?.let { Result.success(it) }
                        ?: Result.failure(IOException("Empty response from OpenAI."))
                }
            }
        } catch (e: Exception) {
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

