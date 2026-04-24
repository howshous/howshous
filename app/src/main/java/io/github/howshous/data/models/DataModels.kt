package io.github.howshous.data.models

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "tenant",
    val gender: String = "",
    val isBanned: Boolean = false,
    val originalRole: String = "",  // Stores the original role when user is banned
    val bannedAt: Timestamp? = null,
    val bannedBy: String = "",
    val banReason: String = "",
    val verified: Boolean = false,
    val profileImageUrl: String = "",
    val businessPermitUrl: String = "",
    val createdAt: Timestamp? = null
)

data class BanAppeal(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val status: String = "pending", // pending, approved, rejected
    val createdAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String = "",
    val reviewNotes: String = ""
)

data class Listing(
    val id: String = "",
    val landlordId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val price: Int = 0,
    val deposit: Int = 0,
    val capacity: Int = 1,
    val currentOccupancy: Int = 0,
    val hasAvailableSlots: Boolean = true,
    val genderPolicy: String = "any", // any, female, male
    val status: String = "under_review", // under_review, rejected, active, inactive, delisted
    val previousStatus: String = "", // active/inactive before under_review
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String = "",
    val reviewNotes: String = "",
    val photos: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val landDeedUrl: String = "",
    val reviewSummary: ListingReviewSummary? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val uniqueViewCount: Int = 0,
    val isSample: Boolean = false,
    val contractTemplate: Map<String, Any>? = null
)

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "text", // "text", "contract", "image"
    val contractId: String = "", // Only used when type is "contract"
    val imageUrl: String = "" // Only used when type is "image"
)

data class Contract(
    val id: String = "",
    val chatId: String = "",
    val listingId: String = "",
    val landlordId: String = "",
    val tenantId: String = "",
    val title: String = "",
    val terms: String = "",
    val monthlyRent: Int = 0,
    val deposit: Int = 0,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val status: String = "pending", // "pending", "signed", "rejected", "terminated"
    val signedAt: Timestamp? = null,
    val createdAt: Timestamp? = null
)

data class Chat(
    val id: String = "",
    val listingId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Timestamp? = null
)

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // "payment_due", "message", "system", "inquiry"
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val notified: Boolean = false,
    val notifiedAt: Timestamp? = null,
    val timestamp: Timestamp? = null,
    val actionUrl: String = "",
    val listingId: String = "",
    val senderId: String = ""
)

data class Rental(
    val id: String = "",
    val listingId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val contractId: String = "",
    val startDate: Timestamp? = null,
    val nextDueDate: Timestamp? = null,
    val status: String = "active",
    val monthlyRent: Int = 0
)

data class Tenancy(
    val id: String = "",
    val listingId: String = "",
    val tenantId: String = "",
    val tenantName: String = "",
    val landlordId: String = "",
    val contractId: String = "",
    val status: String = "signed", // signed, confirmed, needs_resign, ended
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Issue(
    val id: String = "",
    val listingId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val contractId: String = "",
    val chatId: String = "",
    val issueType: String = "",
    val description: String = "",
    val status: String = "pending", // "pending", "resolved"
    val resolutionPhotoUrl: String = "",
    val reportedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
)
