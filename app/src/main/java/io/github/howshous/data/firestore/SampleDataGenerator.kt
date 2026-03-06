package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import io.github.howshous.data.models.*
import java.util.*

object SampleDataGenerator {

    // Sample users
    val sampleTenant = UserProfile(
        uid = "tenant_001",
        firstName = "Rajesh",
        lastName = "Kumar",
        email = "rajesh@example.com",
        phone = "9876543210",
        role = "tenant",
        verified = true,
        profileImageUrl = "",
        createdAt = Timestamp.now()
    )

    val sampleLandlord = UserProfile(
        uid = "landlord_001",
        firstName = "Priya",
        lastName = "Sharma",
        email = "priya@example.com",
        phone = "9123456789",
        role = "landlord",
        verified = true,
        profileImageUrl = "",
        createdAt = Timestamp.now()
    )

    // Sample listings
    val sampleListings = listOf(
        Listing(
            id = "listing_001",
            landlordId = "landlord_001",
            title = "Modern 2BHK Apartment",
            description = "Spacious 2-bedroom apartment with modern amenities in a prime location.",
            location = "Bangalore, Whitefield",
            price = 25000,
            deposit = 50000,
            status = "active",
            reviewStatus = "under_review",
            photos = listOf(),
            amenities = listOf("WiFi", "AC", "Parking", "Water Supply", "Gym"),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        ),
        Listing(
            id = "listing_002",
            landlordId = "landlord_001",
            title = "Cozy 1BHK Flat",
            description = "Compact but comfortable 1-bedroom apartment suitable for working professionals.",
            location = "Bangalore, Koramangala",
            price = 18000,
            deposit = 36000,
            status = "active",
            reviewStatus = "under_review",
            photos = listOf(),
            amenities = listOf("WiFi", "Furnished", "Water Supply"),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        ),
        Listing(
            id = "listing_003",
            landlordId = "landlord_001",
            title = "Premium 3BHK Villa",
            description = "Luxurious 3-bedroom villa with private garden and modern kitchen.",
            location = "Bangalore, Indiranagar",
            price = 45000,
            deposit = 90000,
            status = "full",
            reviewStatus = "under_review",
            photos = listOf(),
            amenities = listOf("WiFi", "AC", "Parking", "Garden", "Pool", "Gym"),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    )

    // Sample chat
    val sampleChat = Chat(
        id = "chat_001",
        listingId = "listing_001",
        tenantId = "tenant_001",
        landlordId = "landlord_001",
        lastMessage = "When can we schedule a viewing?",
        lastTimestamp = Timestamp.now()
    )

    // Sample messages
    val sampleMessages = listOf(
        Message(
            id = "msg_001",
            chatId = "chat_001",
            senderId = "tenant_001",
            text = "Hi, I'm interested in the 2BHK apartment. Is it still available?",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 300000))
        ),
        Message(
            id = "msg_002",
            chatId = "chat_001",
            senderId = "landlord_001",
            text = "Yes, it's available. When would you like to view it?",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 180000))
        ),
        Message(
            id = "msg_003",
            chatId = "chat_001",
            senderId = "tenant_001",
            text = "How about this weekend at 10 AM?",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 60000))
        ),
        Message(
            id = "msg_004",
            chatId = "chat_001",
            senderId = "landlord_001",
            text = "Perfect! I'll see you then. Location: Whitefield, Bangalore.",
            timestamp = Timestamp.now()
        )
    )

    // Sample notifications
    val sampleNotifications = listOf(
        Notification(
            id = "notif_001",
            userId = "tenant_001",
            type = "message",
            title = "New Message",
            message = "Priya replied to your message",
            read = false,
            timestamp = Timestamp.now(),
            actionUrl = "chat/chat_001"
        ),
        Notification(
            id = "notif_002",
            userId = "tenant_001",
            type = "inquiry",
            title = "Viewing Scheduled",
            message = "Your viewing for Modern 2BHK is confirmed for Saturday 10 AM",
            read = true,
            timestamp = Timestamp(Date(System.currentTimeMillis() - 3600000)),
            actionUrl = "listing/listing_001"
        ),
        Notification(
            id = "notif_003",
            userId = "landlord_001",
            type = "payment_due",
            title = "Rent Due",
            message = "Rent is due in 5 days",
            read = false,
            timestamp = Timestamp(Date(System.currentTimeMillis() - 7200000)),
            actionUrl = ""
        )
    )

    // Sample rental
    val sampleRental = Rental(
        id = "rental_001",
        listingId = "listing_001",
        tenantId = "tenant_001",
        landlordId = "landlord_001",
        contractId = "contract_001",
        nextDueDate = Timestamp(Date(System.currentTimeMillis() + 86400000 * 10)),
        status = "active",
        monthlyRent = 25000
    )

    suspend fun seedSampleData(
        userRepo: UserRepository,
        listingRepo: ListingRepository,
        chatRepo: ChatRepository,
        notificationRepo: NotificationRepository,
        rentalRepo: RentalRepository
    ) {
        try {
            // Add users
            userRepo.updateUserProfile(sampleTenant.uid, mapOf(
                "firstName" to sampleTenant.firstName,
                "lastName" to sampleTenant.lastName,
                "email" to sampleTenant.email,
                "phone" to sampleTenant.phone,
                "role" to sampleTenant.role,
                "verified" to sampleTenant.verified
            ))

            userRepo.updateUserProfile(sampleLandlord.uid, mapOf(
                "firstName" to sampleLandlord.firstName,
                "lastName" to sampleLandlord.lastName,
                "email" to sampleLandlord.email,
                "phone" to sampleLandlord.phone,
                "role" to sampleLandlord.role,
                "verified" to sampleLandlord.verified
            ))

            // Add listings (they have their own IDs set)
            println("Sample data seeded successfully")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
