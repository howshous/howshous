package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SavedListingsRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun userSavesCollection(userId: String) =
        db.collection("users").document(userId).collection("saved_listings")

    suspend fun isListingSaved(userId: String, listingId: String): Boolean {
        if (userId.isBlank() || listingId.isBlank()) return false
        return try {
            val doc = userSavesCollection(userId).document(listingId).get().await()
            doc.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveListing(
        userId: String,
        listingId: String,
        price: Int?
    ) {
        if (userId.isBlank() || listingId.isBlank()) return
        runCatching {
            val data = hashMapOf<String, Any?>(
                "listingId" to listingId,
                "savedAt" to Timestamp.now()
            ).apply {
                price?.let { put("priceAtSave", it) }
            }
            userSavesCollection(userId).document(listingId).set(data).await()
        }
    }

    suspend fun unsaveListing(userId: String, listingId: String) {
        if (userId.isBlank() || listingId.isBlank()) return
        runCatching {
            userSavesCollection(userId).document(listingId).delete().await()
        }
    }
}

