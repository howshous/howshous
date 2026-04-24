package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.github.howshous.data.models.UserProfile
import io.github.howshous.data.models.BanAppeal
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
            val userDoc = db.collection("users").document(uid).get().await()
            val currentRole = userDoc.getString("role") ?: ""
            val originalRole = userDoc.getString("originalRole")?.takeIf { it.isNotBlank() } ?: currentRole
            
            val updates = if (banned) {
                mapOf(
                    "role" to "banned",
                    "originalRole" to originalRole,
                    "isBanned" to true,
                    "bannedAt" to Timestamp.now(),
                    "bannedBy" to adminUid,
                    "banReason" to reason
                )
            } else {
                mapOf(
                    "role" to originalRole,
                    "isBanned" to false,
                    "bannedAt" to null,
                    "bannedBy" to "",
                    "banReason" to ""
                )
            }
            db.collection("users").document(uid).update(updates).await()
            
            if (originalRole == "landlord") {
                if (banned) {
                    delistLandlordListings(uid, adminUid, reason)
                } else {
                    resetLandlordListingsForReview(uid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun delistLandlordListings(
        landlordId: String,
        adminUid: String,
        reason: String
    ) {
        if (landlordId.isBlank()) return
        try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            val note = reason.trim().ifBlank { "Delisted due to landlord ban." }
            val now = Timestamp.now()
            for (doc in snap.documents) {
                val currentStatus = doc.getString("status") ?: ""
                val existingPrevious = doc.getString("previousStatus") ?: ""
                val previousStatus = if (currentStatus == "active" || currentStatus == "inactive") {
                    currentStatus
                } else {
                    existingPrevious
                }
                doc.reference.update(
                    mapOf(
                        "status" to "delisted",
                        "previousStatus" to previousStatus,
                        "reviewedAt" to now,
                        "reviewedBy" to adminUid,
                        "reviewNotes" to note
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun resetLandlordListingsForReview(landlordId: String) {
        if (landlordId.isBlank()) return
        try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.update(
                    mapOf(
                        "status" to "under_review",
                        "reviewedAt" to null,
                        "reviewedBy" to "",
                        "reviewNotes" to ""
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class BanAppealRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAppealsForUser(uid: String): List<BanAppeal> {
        return try {
            val snap = db.collection("ban_appeals")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(BanAppeal::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun hasPendingAppeal(uid: String): Boolean {
        return try {
            val snap = db.collection("ban_appeals")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents.any { it.getString("status") == "pending" }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createAppeal(uid: String, message: String): Result<String> {
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Appeal message cannot be empty."))
        }
        return try {
            if (hasPendingAppeal(uid)) {
                return Result.failure(IllegalStateException("You already have a pending appeal."))
            }
            val data = mapOf(
                "userId" to uid,
                "message" to message.trim(),
                "status" to "pending",
                "createdAt" to Timestamp.now(),
                "reviewedAt" to null,
                "reviewedBy" to "",
                "reviewNotes" to ""
            )
            val docRef = db.collection("ban_appeals").add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAppeals(limit: Long = 200): List<BanAppeal> {
        return try {
            val snap = db.collection("ban_appeals")
                .limit(limit)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(BanAppeal::class.java)?.copy(id = doc.id)
            }.sortedWith(compareBy<BanAppeal> { it.status != "pending" }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L })
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPendingCount(): Int {
        return try {
            val snap = db.collection("ban_appeals")
                .whereEqualTo("status", "pending")
                .get()
                .await()
            snap.size()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun reviewAppeal(
        appealId: String,
        adminUid: String,
        approved: Boolean,
        notes: String = ""
    ): Boolean {
        return try {
            val appealRef = db.collection("ban_appeals").document(appealId)
            val appealDoc = appealRef.get().await()
            if (!appealDoc.exists()) {
                return false
            }
            val userId = appealDoc.getString("userId") ?: return false
            val status = if (approved) "approved" else "rejected"

            db.runTransaction { transaction ->
                transaction.update(
                    appealRef,
                    mapOf(
                        "status" to status,
                        "reviewedAt" to Timestamp.now(),
                        "reviewedBy" to adminUid,
                        "reviewNotes" to notes.trim()
                    )
                )
            }.await()

            if (approved) {
                UserRepository().setUserBanStatus(
                    uid = userId,
                    banned = false,
                    adminUid = adminUid,
                    reason = ""
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val cacheTtlMs = 30_000L
    private val tenantGenderCache = mutableMapOf<String, String>()
    private val tenantListingsCache = mutableMapOf<String, Pair<Long, List<Listing>>>()

    private suspend fun getTenantGender(tenantId: String): String {
        if (tenantId.isBlank()) return ""
        tenantGenderCache[tenantId]?.let { return it }
        return try {
            val gender = db.collection("users").document(tenantId).get().await()
                .getString("gender")
                .orEmpty()
                .trim()
                .lowercase()
            tenantGenderCache[tenantId] = gender
            gender
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Fetches active listings then filters by open slots + gender policy in app.
     * (Firestore query uses only [status] so legacy docs without genderPolicy/hasAvailableSlots still appear.)
     */
    suspend fun getAllListingsForTenant(tenantId: String): List<Listing> {
        val now = System.currentTimeMillis()
        tenantListingsCache[tenantId]?.let { (cachedAt, cachedListings) ->
            if (now - cachedAt <= cacheTtlMs) return cachedListings
        }

        val tenantGender = getTenantGender(tenantId)

        return try {
            val snap = db.collection("listings")
                .whereEqualTo("status", "active")
                .get()
                .await()
            val listings = snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }.filter { listingMatchesTenantListingFilters(it, tenantGender) }
            tenantListingsCache[tenantId] = now to listings
            listings
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun listingMatchesTenantListingFilters(listing: Listing, tenantGender: String): Boolean {
        val maxCap = listing.capacity.coerceAtLeast(1)
        val occ = listing.currentOccupancy.coerceAtLeast(0).coerceAtMost(maxCap)
        val hasOpenSlots = occ < maxCap
        if (!hasOpenSlots) return false

        val policy = listing.genderPolicy.trim().lowercase().ifBlank { "any" }
        return when {
            policy == "any" -> true
            tenantGender == "female" || tenantGender == "male" -> policy == tenantGender
            else -> policy == "any"
        }
    }

    /** Landlord-only: fills missing occupancy/gender fields on this landlord's listings (Firestore rules allow it). */
    suspend fun backfillLandlordListingVisibility(landlordId: String, limit: Long = 200): Int {
        if (landlordId.isBlank()) return 0
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .limit(limit)
                .get()
                .await()

            var updatedCount = 0
            for (doc in snap.documents) {
                val listingId = doc.id
                if (doc.getString("genderPolicy").isNullOrBlank()) {
                    doc.reference.update("genderPolicy", "any").await()
                    updatedCount++
                }
                syncListingOccupancy(listingId)
            }
            if (updatedCount > 0) tenantListingsCache.clear()
            updatedCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
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
                .whereEqualTo("status", "under_review")
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
                .whereEqualTo("status", "active")
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

    fun listenToListing(
        id: String,
        onUpdate: (Listing?) -> Unit
    ): ListenerRegistration? {
        if (id.isBlank()) return null
        return db.collection("listings").document(id)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    err.printStackTrace()
                    onUpdate(null)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                val listing = snap.toObject(Listing::class.java)?.copy(id = snap.id)
                onUpdate(listing)
            }
    }

    suspend fun createListing(listing: Listing): String {
        return try {
            val normalizedCapacity = listing.capacity.coerceAtLeast(1)
            val normalizedOccupancy = listing.currentOccupancy.coerceAtLeast(0).coerceAtMost(normalizedCapacity)
            val forWrite = listing.copy(
                id = "",
                capacity = normalizedCapacity,
                currentOccupancy = normalizedOccupancy,
                hasAvailableSlots = normalizedOccupancy < normalizedCapacity,
                genderPolicy = listing.genderPolicy.trim().lowercase().ifBlank { "any" },
                status = "under_review",
                previousStatus = if (listing.status == "inactive") "inactive" else listing.previousStatus,
                reviewedAt = null,
                reviewedBy = "",
                reviewNotes = "",
                reviewSummary = listing.reviewSummary ?: io.github.howshous.data.models.ListingReviewSummary()
            )
            if (listing.id.isNotBlank()) {
                db.collection("listings").document(listing.id).set(forWrite).await()
                tenantListingsCache.clear()
                listing.id
            } else {
                val ref = db.collection("listings").add(forWrite).await()
                tenantListingsCache.clear()
                ref.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun syncListingOccupancy(listingId: String): Int {
        if (listingId.isBlank()) return 0
        return try {
            val listingRef = db.collection("listings").document(listingId)
            val listingSnap = listingRef.get().await()
            val capacity = (listingSnap.getLong("capacity")?.toInt() ?: 1).coerceAtLeast(1)

            val tenanciesSnap = db.collection("tenancies")
                .whereEqualTo("listingId", listingId)
                .get()
                .await()

            val activeStatuses = setOf("signed", "confirmed", "needs_resign", "active")
            val occupied = tenanciesSnap.documents.count { doc ->
                val status = doc.getString("status").orEmpty().lowercase()
                activeStatuses.contains(status)
            }
            val normalized = occupied.coerceAtMost(capacity)

            listingRef.update(
                mapOf(
                    "currentOccupancy" to normalized,
                    "hasAvailableSlots" to (normalized < capacity)
                )
            ).await()
            tenantListingsCache.clear()
            normalized
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun updateListing(id: String, updates: Map<String, Any>) {
        try {
            db.collection("listings").document(id).update(updates).await()
            tenantListingsCache.clear()
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
        try {
            val doc = db.collection("listings").document(listingId).get().await()
            val previousStatus = doc.getString("previousStatus") ?: ""
            val nextStatus = if (previousStatus == "inactive") "inactive" else "active"
            updateListing(
                listingId,
                mapOf(
                    "status" to nextStatus,
                    "previousStatus" to "",
                    "reviewedAt" to Timestamp.now(),
                    "reviewedBy" to adminUid,
                    "reviewNotes" to notes
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun rejectListing(listingId: String, adminUid: String, notes: String = "") {
        updateListing(
            listingId,
            mapOf(
                "status" to "rejected",
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
                "status" to "delisted",
                "reviewedAt" to Timestamp.now(),
                "reviewedBy" to adminUid,
                "reviewNotes" to notes
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
                "notified" to false,
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

    fun listenNotificationsForUser(
        userId: String,
        limit: Long = 20,
        onUpdate: (List<Notification>) -> Unit
    ): ListenerRegistration? {
        if (userId.isBlank()) return null
        return db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    error?.printStackTrace()
                    return@addSnapshotListener
                }
                val notifications = snap.documents.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                }
                onUpdate(notifications)
            }
    }

    suspend fun markNotified(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "notified" to true,
                    "notifiedAt" to Timestamp.now()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
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
    private val listingRepository = ListingRepository()

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
            listingRepository.syncListingOccupancy(listingId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
