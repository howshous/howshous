package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AIChatMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val isTenant: Boolean = true, // true for tenant, false for AI
    val timestamp: Timestamp? = null
)

class AIChatRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun docId(userId: String, chatKey: String?): String =
        if (chatKey.isNullOrBlank()) userId else "${chatKey}_$userId"

    suspend fun saveMessage(userId: String, text: String, isTenant: Boolean) {
        saveMessage(userId, text, isTenant, null)
    }

    suspend fun saveMessage(userId: String, text: String, isTenant: Boolean, chatKey: String? = null) {
        try {
            val message = hashMapOf(
                "userId" to userId,
                "text" to text,
                "isTenant" to isTenant,
                "timestamp" to Timestamp.now()
            )
            db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .add(message)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadMessages(userId: String): List<AIChatMessage> {
        return loadMessages(userId, null)
    }

    suspend fun loadMessages(userId: String, chatKey: String? = null): List<AIChatMessage> {
        return try {
            val snap = db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .orderBy("timestamp")
                .get()
                .await()
            
            snap.documents.mapNotNull { doc ->
                AIChatMessage(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    text = doc.getString("text") ?: "",
                    isTenant = doc.getBoolean("isTenant") ?: true,
                    timestamp = doc.getTimestamp("timestamp")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteChatHistory(userId: String, chatKey: String? = null): Boolean {
        try {
            val snap = db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun initializeWelcomeMessage(userId: String) {
        val existingMessages = loadMessages(userId)
        if (existingMessages.isEmpty()) {
            saveMessage(
                userId = userId,
                text = "Hi! I'm your boarding house assistant for Baguio City. Tell me your budget, preferred location in Baguio, and must-have amenities and I'll point you to the best matches.",
                isTenant = false
            )
        }
    }

    suspend fun initializeLandlordAnalyticsWelcome(userId: String) {
        val existingMessages = loadMessages(userId, "landlord_analytics")
        if (existingMessages.isEmpty()) {
            saveMessage(
                userId = userId,
                text = "Insights based on recent activity. Ask about your views, saves, and conversion rates for possible improvements (not guarantees).",
                isTenant = false,
                chatKey = "landlord_analytics"
            )
        }
    }
}

