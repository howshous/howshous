package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.AIChatRepository
import io.github.howshous.data.firestore.ListingMetricsRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.util.GroqApiClient
import io.github.howshous.utils.ReviewSummaryUtils
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LandlordAnalyticsAIViewModel : ViewModel() {
    private val aiChatRepository = AIChatRepository()
    private val listingRepository = ListingRepository()
    private val metricsRepository = ListingMetricsRepository()
    private val chatKey = "landlord_analytics"

    private val _messages = MutableStateFlow<List<TenantAIMessage>>(emptyList())
    val messages: StateFlow<List<TenantAIMessage>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private var currentUserId: String = ""
    private val requestTimeoutMs = 20_000L

    fun initializeChat(userId: String) {
        if (currentUserId == userId && _messages.value.isNotEmpty()) return
        currentUserId = userId
        viewModelScope.launch {
            aiChatRepository.initializeLandlordAnalyticsWelcome(userId)
            val saved = aiChatRepository.loadMessages(userId, chatKey)
            _messages.value = saved.mapIndexed { idx, msg ->
                TenantAIMessage(
                    id = msg.timestamp?.toDate()?.time ?: (System.currentTimeMillis() + idx),
                    author = if (msg.isTenant) MessageAuthor.TENANT else MessageAuthor.AI,
                    text = msg.text
                )
            }
        }
    }

    private suspend fun buildLandlordContextJson(): String {
        if (currentUserId.isBlank()) return "{}"
        val listings = listingRepository.getListingsForLandlord(currentUserId)
        val metrics = metricsRepository.getMetricsForListings(listings.map { it.id })
        val totalViews = metrics.values.sumOf { it.views30d }
        val totalSaves = metrics.values.sumOf { it.saves30d }
        val totalMessages = metrics.values.sumOf { it.messages30d }
        val totalReviews = listings.sumOf { it.reviewSummary?.total ?: 0 }
        val totalRecommended = listings.sumOf { it.reviewSummary?.recommendedCount ?: 0 }
        val overallRecommendPct = if (totalReviews > 0) {
            "%.1f".format(totalRecommended * 100f / totalReviews)
        } else {
            "0.0"
        }
        val arr = JSONArray()
        listings
            .sortedByDescending { metrics[it.id]?.views30d ?: 0 }
            .take(25)
            .forEach { listing ->
            val m = metrics[listing.id] ?: return@forEach
            val reviewDisplay = ReviewSummaryUtils.buildDisplay(listing.reviewSummary)
            arr.put(JSONObject().apply {
                put("title", listing.title.take(80))
                put("price", listing.price)
                put("views30d", m.views30d)
                put("saves30d", m.saves30d)
                put("messages30d", m.messages30d)
                put("saveRatePct", if (m.views30d > 0) "%.1f".format(m.saves30d * 100f / m.views30d) else "0.0")
                put("messageRatePct", if (m.views30d > 0) "%.1f".format(m.messages30d * 100f / m.views30d) else "0.0")
                put("reviewPercent", reviewDisplay.recommendedPercent)
                put("reviewLabel", reviewDisplay.label)
                put("reviewTotal", reviewDisplay.total)
            })
        }
        return JSONObject().apply {
            put("summary", JSONObject().apply {
                put("totalViews30d", totalViews)
                put("totalSaves30d", totalSaves)
                put("totalMessages30d", totalMessages)
                put("totalReviews", totalReviews)
                put("overallRecommendPct", overallRecommendPct)
            })
            put("windowDays", 30)
            put("listings", arr)
        }.toString()
    }

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isThinking.value || currentUserId.isBlank()) return
        _messages.value = _messages.value + TenantAIMessage(
            id = System.currentTimeMillis(),
            author = MessageAuthor.TENANT,
            text = trimmed
        )
        viewModelScope.launch {
            aiChatRepository.saveMessage(currentUserId, trimmed, true, chatKey)
            _isThinking.value = true
            val reply = try {
                withTimeoutOrNull(requestTimeoutMs) {
                    val ctx = buildLandlordContextJson()
                    GroqApiClient.fetchLandlordAnalyticsReply(trimmed, ctx).getOrElse { err ->
                        val msg = err.message?.lowercase().orEmpty()
                        when {
                            msg.contains("invalid_refresh_token") || msg.contains("unauthenticated") ->
                                "Session expired. Please log out and log back in, then try again."
                            msg.contains("api key") || msg.contains("groq_api_key") ->
                                "AI is not configured on this build. Add GROQ_API_KEY in local.properties."
                            else -> "Insights temporarily unavailable. Check Performance tab for raw analytics."
                        }
                    }
                } ?: "Insight request timed out. Please try again."
            } catch (e: Exception) {
                val msg = e.message?.lowercase().orEmpty()
                when {
                    msg.contains("invalid_refresh_token") || msg.contains("unauthenticated") ->
                        "Session expired. Please log out and log back in, then try again."
                    else -> "Insights temporarily unavailable. Check Performance tab for raw analytics."
                }
            }
            _messages.value = _messages.value + TenantAIMessage(
                id = System.currentTimeMillis(),
                author = MessageAuthor.AI,
                text = reply
            )
            _isThinking.value = false
            aiChatRepository.saveMessage(currentUserId, reply, false, chatKey)
        }
    }

    fun clearChatHistory() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            val ok = aiChatRepository.deleteChatHistory(currentUserId, chatKey)
            _messages.value = emptyList()
            if (ok) {
                aiChatRepository.initializeLandlordAnalyticsWelcome(currentUserId)
                val saved = aiChatRepository.loadMessages(currentUserId, chatKey)
                _messages.value = saved.mapIndexed { idx, msg ->
                    TenantAIMessage(
                        id = msg.timestamp?.toDate()?.time ?: (System.currentTimeMillis() + idx),
                        author = if (msg.isTenant) MessageAuthor.TENANT else MessageAuthor.AI,
                        text = msg.text
                    )
                }
            } else {
                _messages.value = listOf(
                    TenantAIMessage(
                        id = System.currentTimeMillis(),
                        author = MessageAuthor.AI,
                        text = "Insights based on recent activity. Ask about your views, saves, and conversion rates for possible improvements (not guarantees)."
                    )
                )
            }
        }
    }
}
