package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TenantAIMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String
)

enum class MessageAuthor { TENANT, AI }

class TenantAIHelperViewModel : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            TenantAIMessage(
                id = System.currentTimeMillis(),
                author = MessageAuthor.AI,
                text = "Hi! I'm your boarding house assistant. Tell me your budget, preferred location, and must-have amenities and I'll point you to the best matches."
            )
        )
    )
    val messages: StateFlow<List<TenantAIMessage>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isThinking.value) return

        val userMessage = TenantAIMessage(
            id = System.currentTimeMillis(),
            author = MessageAuthor.TENANT,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isThinking.value = true
            delay(1000)
            val aiReply = TenantAIMessage(
                id = System.currentTimeMillis(),
                author = MessageAuthor.AI,
                text = buildResponse(trimmed)
            )
            _messages.value = _messages.value + aiReply
            _isThinking.value = false
        }
    }

    private fun buildResponse(prompt: String): String {
        val suggestions = mutableListOf<String>()

        if (Regex("\\d").containsMatchIn(prompt)) {
            suggestions += "I'll prioritize listings that are within your stated budget."
        }
        if (prompt.contains("near", ignoreCase = true) || prompt.contains("close to", ignoreCase = true)) {
            suggestions += "I'll filter homes near the landmarks or districts you mentioned."
        }
        if (prompt.contains("wifi", ignoreCase = true)) {
            suggestions += "I'll look for homes tagged with reliable Wi‑Fi."
        }

        val base = "Here’s what I’ll do based on your request: ${suggestions.ifEmpty { listOf("I'll scan the latest listings and highlight the ones that best match your preferences.") }.joinToString(" ")}"

        return base + "\n\nTap any recommended listing from the search tab afterwards, or keep sharing more details for a finer recommendation."
    }
}

