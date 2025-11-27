package io.github.howshous.ui.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.Listing
import kotlinx.coroutines.tasks.await

object SampleListingsGenerator {
    
    private val sampleListings = listOf(
        Listing(
            landlordId = "sample_landlord_1",
            title = "Cozy Studio Near Session Road",
            description = "Bright and airy studio unit perfect for students or young professionals. Walking distance to Session Road, SM Baguio, and restaurants. Fully furnished with basic appliances.",
            location = "Session Road",
            price = 4500,
            deposit = 9000,
            status = "active",
            amenities = listOf("WiFi", "Air Conditioning", "Furnished", "Near Public Transport", "Security")
        ),
        Listing(
            landlordId = "sample_landlord_2",
            title = "Spacious Room with Private Bathroom",
            description = "Large room with private bathroom in a quiet neighborhood. Shared kitchen and living area. Perfect for professionals working in Baguio City Center.",
            location = "Baguio City Center",
            price = 8000,
            deposit = 16000,
            status = "active",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "CCTV", "Security")
        ),
        Listing(
            landlordId = "sample_landlord_3",
            title = "Budget-Friendly Boarding House",
            description = "Affordable room in a friendly boarding house. Shared facilities, clean and well-maintained. Great for students on a tight budget.",
            location = "Irisan",
            price = 3000,
            deposit = 6000,
            status = "active",
            amenities = listOf("WiFi", "Kitchen Access", "Laundry", "Security")
        ),
        Listing(
            landlordId = "sample_landlord_4",
            title = "Premium Room with Gym Access",
            description = "Luxury boarding house with modern amenities. Access to gym and swimming pool. Perfect for professionals who value comfort and convenience.",
            location = "Camp John Hay",
            price = 12000,
            deposit = 24000,
            status = "active",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        ),
        Listing(
            landlordId = "sample_landlord_5",
            title = "Pet-Friendly Boarding House",
            description = "Comfortable room in a pet-friendly environment. Your furry friends are welcome! Close to Burnham Park and pet stores.",
            location = "Burnham Park",
            price = 6000,
            deposit = 12000,
            status = "active",
            amenities = listOf("Pets Allowed", "Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "Security")
        ),
        Listing(
            landlordId = "sample_landlord_6",
            title = "Student-Friendly Dormitory",
            description = "Clean and safe dormitory near universities. Study areas available. Strict security for peace of mind.",
            location = "Legarda Road",
            price = 3500,
            deposit = 7000,
            status = "active",
            amenities = listOf("WiFi", "Air Conditioning", "Security", "CCTV", "Near Public Transport")
        ),
        Listing(
            landlordId = "sample_landlord_7",
            title = "Modern Studio with Balcony",
            description = "Newly renovated studio with private balcony. Modern design, fully furnished. Perfect for young professionals.",
            location = "Aurora Hill",
            price = 9500,
            deposit = 19000,
            status = "active",
            amenities = listOf("WiFi", "Air Conditioning", "Furnished", "Kitchen Access", "Laundry", "Security", "CCTV")
        ),
        Listing(
            landlordId = "sample_landlord_8",
            title = "Affordable Room Near Business District",
            description = "Simple but comfortable room near major business areas. Easy commute to Session Road and SM Baguio.",
            location = "Lower Magsaysay",
            price = 5000,
            deposit = 10000,
            status = "active",
            amenities = listOf("WiFi", "Air Conditioning", "Near Public Transport", "Security", "Kitchen Access")
        ),
        Listing(
            landlordId = "sample_landlord_9",
            title = "Family-Style Boarding House",
            description = "Homey atmosphere in a family-run boarding house. Shared meals available. Great for those looking for a community feel.",
            location = "Quezon Hill",
            price = 4000,
            deposit = 8000,
            status = "active",
            amenities = listOf("WiFi", "Kitchen Access", "Laundry", "Security", "Near Public Transport")
        ),
        Listing(
            landlordId = "sample_landlord_10",
            title = "Luxury Room with Mountain View",
            description = "Premium room with stunning mountain views. Top-of-the-line amenities and 24/7 security. Perfect for those who want the best.",
            location = "Mines View Park",
            price = 15000,
            deposit = 30000,
            status = "active",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        )
    )
    
    suspend fun generateSampleListings(): List<String> {
        val db = FirebaseFirestore.getInstance()
        val createdIds = mutableListOf<String>()
        
        return try {
            // First, delete all existing sample listings
            deleteExistingSampleListings()
            
            // Then create new sample listings
            sampleListings.forEach { listing ->
                val docRef = db.collection("listings").document()
                val listingMap = hashMapOf<String, Any>(
                    "landlordId" to listing.landlordId,
                    "title" to listing.title,
                    "description" to listing.description,
                    "location" to listing.location,
                    "price" to listing.price,
                    "deposit" to listing.deposit,
                    "status" to listing.status,
                    "amenities" to listing.amenities,
                    "photos" to listing.photos,
                    "createdAt" to Timestamp.now()
                )
                docRef.set(listingMap).await()
                createdIds.add(docRef.id)
            }
            createdIds
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private suspend fun deleteExistingSampleListings() {
        val db = FirebaseFirestore.getInstance()
        try {
            // Get all listings with landlordId starting with "sample_landlord_"
            val sampleLandlordIds = sampleListings.map { it.landlordId }
            
            // Query for each sample landlord ID and delete their listings
            sampleLandlordIds.forEach { landlordId ->
                val query = db.collection("listings")
                    .whereEqualTo("landlordId", landlordId)
                    .get()
                    .await()
                
                // Delete each document
                query.documents.forEach { doc ->
                    doc.reference.delete().await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue even if deletion fails - we'll just overwrite
        }
    }
}

