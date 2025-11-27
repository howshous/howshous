package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.AIChatRepository
import io.github.howshous.ui.util.LocalAIHelper
import org.json.JSONArray
import org.json.JSONObject

data class TenantAIMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String
)

enum class MessageAuthor { TENANT, AI }

class TenantAIHelperViewModel : ViewModel() {

    private val listingRepository = ListingRepository()
    private val aiChatRepository = AIChatRepository()

    private val _messages = MutableStateFlow<List<TenantAIMessage>>(emptyList())
    val messages: StateFlow<List<TenantAIMessage>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking
    
    private var currentUserId: String = ""
    
    fun initializeChat(userId: String) {
        if (currentUserId == userId && _messages.value.isNotEmpty()) {
            // Already loaded for this user
            return
        }
        currentUserId = userId
        viewModelScope.launch {
            // Initialize welcome message if needed
            aiChatRepository.initializeWelcomeMessage(userId)
            // Load existing messages
            loadChatHistory(userId)
        }
    }
    
    private suspend fun loadChatHistory(userId: String) {
        val savedMessages = aiChatRepository.loadMessages(userId)
        _messages.value = savedMessages.mapIndexed { index, aiMsg ->
            TenantAIMessage(
                id = aiMsg.timestamp?.toDate()?.time ?: (System.currentTimeMillis() + index),
                author = if (aiMsg.isTenant) MessageAuthor.TENANT else MessageAuthor.AI,
                text = aiMsg.text
            )
        }
    }

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isThinking.value || currentUserId.isEmpty()) return

        val userMessage = TenantAIMessage(
            id = System.currentTimeMillis(),
            author = MessageAuthor.TENANT,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage
        
        // Save user message to Firestore
        viewModelScope.launch {
            aiChatRepository.saveMessage(currentUserId, trimmed, isTenant = true)
        }

        viewModelScope.launch {
            _isThinking.value = true
            val listingsJson = buildListingsJson()
            // Use local AI helper instead of Gemini API
            val aiMessageText = try {
                LocalAIHelper.generateRecommendation(trimmed, listingsJson)
            } catch (e: Exception) {
                android.util.Log.e("TenantAIHelper", "Local AI error: ${e.message}", e)
                "I encountered an error processing your request. Please try again."
            }
            val aiReply = TenantAIMessage(
                id = System.currentTimeMillis(),
                author = MessageAuthor.AI,
                text = aiMessageText
            )
            _messages.value = _messages.value + aiReply
            _isThinking.value = false
            
            // Save AI reply to Firestore
            aiChatRepository.saveMessage(currentUserId, aiMessageText, isTenant = false)
        }
    }

    private suspend fun buildListingsJson(): String {
        return runCatching {
            val allListings = listingRepository.getAllListings()
            // Filter to only active listings
            val activeListings = allListings.filter { it.status == "active" }
            
            // Build JSON array matching the working example structure
            val jsonArray = JSONArray()
            activeListings.forEach { listing ->
                val listingObj = JSONObject().apply {
                    put("id", listing.id)
                    put("title", listing.title)
                    put("price", listing.price)
                    put("type", "boarding house") // Default type
                    put("beds", 0) // Not in our model, default to 0
                    put("baths", 0) // Not in our model, default to 0
                    put("amenities", JSONArray(listing.amenities))
                    put("description", listing.description)
                    put("location", listing.location)
                }
                jsonArray.put(listingObj)
            }
            jsonArray.toString()
        }.getOrDefault("[]")
    }
}

