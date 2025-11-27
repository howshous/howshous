package io.github.howshous.data.models

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "tenant",
    val verified: Boolean = false,
    val profileImageUrl: String = "",
    val createdAt: Timestamp? = null
)

data class Listing(
    val id: String = "",
    val landlordId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val price: Int = 0,
    val deposit: Int = 0,
    val status: String = "active", // active, full, maintenance
    val photos: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "text", // "text", "contract"
    val contractId: String = "" // Only used when type is "contract"
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
    val status: String = "pending", // "pending", "signed", "rejected"
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
    val timestamp: Timestamp? = null,
    val actionUrl: String = ""
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