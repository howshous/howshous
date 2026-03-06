package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.github.howshous.data.models.UserProfile
import io.github.howshous.data.models.Listing
import io.github.howshous.data.models.Chat
import io.github.howshous.data.models.Message
import io.github.howshous.data.models.Notification
import io.github.howshous.data.models.Rental
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val profile = doc.toObject(UserProfile::class.java)?.copy(uid = doc.id) ?: return null

            val verificationDoc = try {
                db.collection("verifications").document(uid).get().await()
            } catch (_: Exception) {
                null
            }
            val status = verificationDoc?.getString("status")?.lowercase()
            val verifiedFlag = verificationDoc?.getBoolean("verified") ?: false
            val isVerified = verifiedFlag || status == "approved" || status == "verified" || status == "accepted"

            if (verificationDoc != null && isVerified && !profile.verified) {
                db.collection("users").document(uid).update("verified", true).await()
                return profile.copy(verified = true)
            }

            profile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        try {
            db.collection("users").document(uid).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getVerificationIdUrl(uid: String): String {
        return try {
            val doc = db.collection("verifications").document(uid).get().await()
            doc.getString("idUrl") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun getAllUsers(limit: Long = 200): List<UserProfile> {
        return try {
            val snap = db.collection("users")
                .limit(limit)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun setUserBanStatus(
        uid: String,
        banned: Boolean,
        adminUid: String,
        reason: String = ""
    ) {
        try {
            val updates = if (banned) {
                mapOf(
                    "isBanned" to true,
                    "bannedAt" to Timestamp.now(),
                    "bannedBy" to adminUid,
                    "banReason" to reason
                )
            } else {
                mapOf(
                    "isBanned" to false,
                    "bannedAt" to null,
                    "bannedBy" to "",
                    "banReason" to ""
                )
            }
            db.collection("users").document(uid).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val publicStatuses = listOf("active", "full", "maintenance")

    suspend fun getAllListings(): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("reviewStatus", "approved")
                .whereIn("status", publicStatuses)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }.filter { it.status != "removed" }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllListingsForAdmin(): List<Listing> {
        return try {
            val snap = db.collection("listings").get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListingsUnderReview(): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("reviewStatus", "under_review")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListingsByLocation(location: String): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("location", location)
                .whereEqualTo("reviewStatus", "approved")
                .whereIn("status", publicStatuses)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }.filter { it.status != "removed" }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListingsForLandlord(landlordId: String): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListing(id: String): Listing? {
        return try {
            val doc = db.collection("listings").document(id).get().await()
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createListing(listing: Listing): String {
        return try {
            val forWrite = listing.copy(
                id = "",
                reviewStatus = "under_review",
                reviewedAt = null,
                reviewedBy = "",
                reviewNotes = ""
            )
            if (listing.id.isNotBlank()) {
                db.collection("listings").document(listing.id).set(forWrite).await()
                listing.id
            } else {
                val ref = db.collection("listings").add(forWrite).await()
                ref.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun updateListing(id: String, updates: Map<String, Any>) {
        try {
            db.collection("listings").document(id).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun backfillUniqueViewCountsForLandlord(landlordId: String): Int {
        if (landlordId.isBlank()) return 0

        return try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()

            var updated = 0
            for (doc in snap.documents) {
                val viewsSnap = doc.reference.collection("views").get().await()
                val count = viewsSnap.size()
                val current = doc.getLong("uniqueViewCount")?.toInt() ?: 0
                if (count != current) {
                    doc.reference.update("uniqueViewCount", count).await()
                    updated++
                }
            }
            updated
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun recordUniqueView(listingId: String, viewerId: String) {
        if (listingId.isBlank() || viewerId.isBlank()) return

        try {
            val listingRef = db.collection("listings").document(listingId)
            val viewRef = listingRef.collection("views").document(viewerId)

            db.runTransaction { txn ->
                val viewSnap = txn.get(viewRef)
                if (!viewSnap.exists()) {
                    txn.set(
                        viewRef,
                        mapOf(
                            "viewerId" to viewerId,
                            "viewedAt" to Timestamp.now()
                        )
                    )
                    txn.update(listingRef, "uniqueViewCount", FieldValue.increment(1))
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun approveListing(listingId: String, adminUid: String, notes: String = "") {
        updateListing(
            listingId,
            mapOf(
                "reviewStatus" to "approved",
                "reviewedAt" to Timestamp.now(),
                "reviewedBy" to adminUid,
                "reviewNotes" to notes
            )
        )
    }

    suspend fun rejectListing(listingId: String, adminUid: String, notes: String = "") {
        updateListing(
            listingId,
            mapOf(
                "reviewStatus" to "rejected",
                "reviewedAt" to Timestamp.now(),
                "reviewedBy" to adminUid,
                "reviewNotes" to notes
            )
        )
    }

    suspend fun takeDownListing(listingId: String, adminUid: String, notes: String = "") {
        updateListing(
            listingId,
            mapOf(
                "reviewStatus" to "taken_down",
                "reviewedAt" to Timestamp.now(),
                "reviewedBy" to adminUid,
                "reviewNotes" to notes,
                "status" to "removed"
            )
        )
    }

}

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val analyticsRepo = AnalyticsRepository()

    suspend fun getChatsForUser(userId: String): List<Chat> {
        return try {
            // Query chats where user is either tenant or landlord
            val tenantChats = db.collection("chats")
                .whereEqualTo("tenantId", userId)
                .get()
                .await()
            
            val landlordChats = db.collection("chats")
                .whereEqualTo("landlordId", userId)
                .get()
                .await()
            
            val allChats = (tenantChats.documents + landlordChats.documents)
                .distinctBy { it.id }
                .mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                }
            
            // Sort by lastTimestamp descending
            allChats.sortedByDescending { it.lastTimestamp?.seconds ?: 0L }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMessagesForChat(chatId: String): List<Message> {
        return try {
            val snap = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        try {
            val timestamp = com.google.firebase.Timestamp.now()
            val message = Message(
                senderId = senderId,
                text = text,
                chatId = chatId,
                timestamp = timestamp,
                type = "text"
            )
            val chatRef = db.collection("chats").document(chatId)
            val messagesRef = chatRef.collection("messages")

            // Determine if this is the first message in this chat (for this listing)
            val existingMessages = messagesRef.limit(1).get().await()
            val isFirstMessageInChat = existingMessages.isEmpty

            // Save message
            messagesRef.add(message).await()
            // Update chat's lastMessage and lastTimestamp
            chatRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastTimestamp" to timestamp
                )
            ).await()

            // Log analytics only for the *first* message for this chat/listing
            if (isFirstMessageInChat) {
                val chatDoc = chatRef.get().await()
                val listingId = chatDoc.getString("listingId") ?: ""
                val landlordId = chatDoc.getString("landlordId") ?: ""
                analyticsRepo.logMessageSent(
                    chatId = chatId,
                    listingId = listingId,
                    landlordId = landlordId,
                    senderId = senderId
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getExistingChatIdForListing(
        listingId: String,
        tenantId: String,
        landlordId: String
    ): String {
        return try {
            val existing = db.collection("chats")
                .whereEqualTo("listingId", listingId)
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("landlordId", landlordId)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                existing.documents[0].id
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun hasMessages(chatId: String): Boolean {
        return try {
            val snap = db.collection("chats").document(chatId).collection("messages")
                .limit(1)
                .get()
                .await()
            !snap.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUrl: String,
        label: String = "Image"
    ) {
        if (imageUrl.isBlank()) return
        try {
            val timestamp = com.google.firebase.Timestamp.now()
            val message = Message(
                senderId = senderId,
                text = label,
                chatId = chatId,
                timestamp = timestamp,
                type = "image",
                imageUrl = imageUrl
            )
            val chatRef = db.collection("chats").document(chatId)
            val messagesRef = chatRef.collection("messages")

            val existingMessages = messagesRef.limit(1).get().await()
            val isFirstMessageInChat = existingMessages.isEmpty

            messagesRef.add(message).await()
            chatRef.update(
                mapOf(
                    "lastMessage" to label,
                    "lastTimestamp" to timestamp
                )
            ).await()

            if (isFirstMessageInChat) {
                val chatDoc = chatRef.get().await()
                val listingId = chatDoc.getString("listingId") ?: ""
                val landlordId = chatDoc.getString("landlordId") ?: ""
                analyticsRepo.logMessageSent(
                    chatId = chatId,
                    listingId = listingId,
                    landlordId = landlordId,
                    senderId = senderId
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getOrCreateChatForListing(
        listingId: String,
        tenantId: String,
        landlordId: String
    ): String {
        return try {
            val existing = db.collection("chats")
                .whereEqualTo("listingId", listingId)
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("landlordId", landlordId)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return existing.documents[0].id
            }

            val chat = Chat(
                id = "",
                listingId = listingId,
                tenantId = tenantId,
                landlordId = landlordId,
                lastMessage = "",
                lastTimestamp = Timestamp.now()
            )
            createChat(chat)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun createChat(chat: Chat): String {
        return try {
            val docRef = db.collection("chats").add(chat).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun getChat(chatId: String): Chat? {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            doc.toObject(Chat::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createNotification(
        userId: String,
        type: String,
        title: String,
        message: String,
        actionUrl: String,
        listingId: String? = null,
        senderId: String? = null
    ) {
        try {
            val data = mutableMapOf<String, Any>(
                "userId" to userId,
                "type" to type,
                "title" to title,
                "message" to message,
                "read" to false,
                "timestamp" to Timestamp.now(),
                "actionUrl" to actionUrl
            )
            if (!listingId.isNullOrBlank()) data["listingId"] = listingId
            if (!senderId.isNullOrBlank()) data["senderId"] = senderId
            db.collection("notifications").add(data).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getNotificationsForUser(userId: String, limit: Long = 20): List<Notification> {
        return try {
            val snap = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update("read", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

class RentalRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRentalsForTenant(tenantId: String): List<Rental> {
        return try {
            val snap = db.collection("rentals")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Rental::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRentalsForLandlord(landlordId: String): List<Rental> {
        return try {
            val snap = db.collection("rentals")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Rental::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}

class TenancyRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getTenanciesForTenant(tenantId: String): List<io.github.howshous.data.models.Tenancy> {
        return try {
            val snap = db.collection("tenancies")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(io.github.howshous.data.models.Tenancy::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTenanciesForListing(listingId: String): List<io.github.howshous.data.models.Tenancy> {
        return try {
            val snap = db.collection("tenancies")
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(io.github.howshous.data.models.Tenancy::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun listenTenanciesForListing(
        listingId: String,
        onUpdate: (List<io.github.howshous.data.models.Tenancy>) -> Unit
    ): ListenerRegistration? {
        if (listingId.isBlank()) return null
        return db.collection("tenancies")
            .whereEqualTo("listingId", listingId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val tenancies = snap.documents.mapNotNull { doc ->
                    doc.toObject(io.github.howshous.data.models.Tenancy::class.java)?.copy(id = doc.id)
                }
                onUpdate(tenancies)
            }
    }

    suspend fun getTenancy(listingId: String, tenantId: String): io.github.howshous.data.models.Tenancy? {
        return try {
            val tenancyId = "${listingId}_$tenantId"
            val doc = db.collection("tenancies").document(tenancyId).get().await()
            doc.toObject(io.github.howshous.data.models.Tenancy::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateTenancyStatus(listingId: String, tenantId: String, status: String) {
        try {
            val tenancyId = "${listingId}_$tenantId"
            db.collection("tenancies").document(tenancyId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
