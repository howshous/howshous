package io.github.howshous.ui.util

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.random.Random

object SampleListingsGenerator {

    private data class SampleListingTemplate(
        val title: String,
        val description: String,
        val location: String,
        val price: Int,
        val deposit: Int,
        val capacity: Int,
        val status: String,
        val amenities: List<String>
    )

    private data class ListingStatistics(
        val uniqueViewCount: Int,
        val inquiryCount: Int,
        val messageCount: Int,
        val contactClickCount: Int,
        val savesCount: Int
    )

    private val sampleListings = listOf(
        SampleListingTemplate(
            "Cozy Studio Near Session Road",
            "Bright and airy studio unit perfect for students or young professionals. Walking distance to Session Road, SM Baguio, and restaurants. Fully furnished with basic appliances.",
            "Session Road",
            4500, 9000, 1, "under_review",
            listOf("WiFi", "Air Conditioning", "Furnished", "Near Public Transport", "Security")
        ),
        SampleListingTemplate(
            "Spacious Room with Private Bathroom",
            "Large room with private bathroom in a quiet neighborhood. Shared kitchen and living area. Perfect for professionals working in Baguio City Center.",
            "Baguio City Center",
            8000, 16000, 1, "under_review",
            listOf("Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "CCTV", "Security")
        ),
        SampleListingTemplate(
            "Budget-Friendly Boarding House",
            "Affordable room in a friendly boarding house. Shared facilities, clean and well-maintained. Great for students on a tight budget.",
            "Irisan",
            3000, 6000, 4, "under_review",
            listOf("WiFi", "Kitchen Access", "Laundry", "Security")
        ),
        SampleListingTemplate(
            "Premium Room with Gym Access",
            "Luxury boarding house with modern amenities. Access to gym and swimming pool. Perfect for professionals who value comfort and convenience.",
            "Camp John Hay",
            12000, 24000, 1, "under_review",
            listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        ),
        SampleListingTemplate(
            "Pet-Friendly Boarding House",
            "Comfortable room in a pet-friendly environment. Your furry friends are welcome! Close to Burnham Park and pet stores.",
            "Burnham Park",
            6000, 12000, 2, "under_review",
            listOf("Pets Allowed", "Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "Security")
        ),
        SampleListingTemplate(
            "Student-Friendly Dormitory",
            "Clean and safe dormitory near universities. Study areas available. Strict security for peace of mind.",
            "Legarda Road",
            3500, 7000, 6, "under_review",
            listOf("WiFi", "Air Conditioning", "Security", "CCTV", "Near Public Transport")
        ),
        SampleListingTemplate(
            "Modern Studio with Balcony",
            "Newly renovated studio with private balcony. Modern design, fully furnished. Perfect for young professionals.",
            "Aurora Hill",
            9500, 19000, 1, "under_review",
            listOf("WiFi", "Air Conditioning", "Furnished", "Kitchen Access", "Laundry", "Security", "CCTV")
        ),
        SampleListingTemplate(
            "Affordable Room Near Business District",
            "Simple but comfortable room near major business areas. Easy commute to Session Road and SM Baguio.",
            "Lower Magsaysay",
            5000, 10000, 1, "under_review",
            listOf("WiFi", "Air Conditioning", "Near Public Transport", "Security", "Kitchen Access")
        ),
        SampleListingTemplate(
            "Family-Style Boarding House",
            "Homey atmosphere in a family-run boarding house. Shared meals available. Great for those looking for a community feel.",
            "Quezon Hill",
            4000, 8000, 3, "under_review",
            listOf("WiFi", "Kitchen Access", "Laundry", "Security", "Near Public Transport")
        ),
        SampleListingTemplate(
            "Luxury Room with Mountain View",
            "Premium room with stunning mountain views. Top-of-the-line amenities and 24/7 security. Perfect for those who want the best.",
            "Mines View Park",
            15000, 30000, 1, "under_review",
            listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        )
    )

    private val reviewCounts = listOf(
        48 to 2,  // 96% overwhelmingly positive
        18 to 6,  // 75% mostly positive
        10 to 12, // 45% mixed
        30 to 3,  // 91% very positive
        14 to 9,  // 61% mixed
        8 to 14,  // 36% mostly negative
        22 to 4,  // 85% positive
        12 to 3,  // 80% positive
        6 to 9,   // 40% mixed
        38 to 1   // 97% overwhelmingly positive
    )

    private val positiveComments = listOf(
        "Clean, quiet, and exactly as described.",
        "Great value for the price. Would recommend.",
        "Owner was responsive and helpful.",
        "Comfortable stay and solid amenities.",
        "Felt safe and convenient for work."
    )

    private val negativeComments = listOf(
        "Needs better maintenance in common areas.",
        "Not as quiet as expected during weekends.",
        "Photos looked better than the actual room.",
        "WiFi was unstable at times.",
        "Security could be improved."
    )

    private fun generateRandomStatistics(price: Int, capacity: Int, amenitiesCount: Int): ListingStatistics {
        val random = Random(System.currentTimeMillis())
        
        // Premium listings (higher price, more amenities) get more engagement
        val qualityScore = (price / 1000.0) + (capacity * 0.5) + (amenitiesCount * 0.2)
        
        // Views: 50-300 base range scaled by quality
        val minViews = 50
        val maxViews = (minViews + qualityScore * 80).toInt()
        val uniqueViewCount = random.nextInt(minViews, maxViews + 1)
        
        // Inquiries: ~1 inquiry per 6-10 views, with variance
        val baseInquiries = (uniqueViewCount / (12 - qualityScore.toInt())).coerceAtLeast(2)
        val inquiryVariance = (baseInquiries * 0.5).toInt().coerceAtLeast(1)
        val inquiryCount = baseInquiries + random.nextInt(-inquiryVariance, inquiryVariance + 1)
        
        // Messages: typically 1.2x to 1.8x inquiries
        val messageMultiplier = 1.2 + (random.nextDouble() * 0.6)
        val messageCount = (inquiryCount * messageMultiplier).toInt().coerceAtLeast(inquiryCount)
        
        // Contact clicks: ~15-25% of views
        val contactClickRatio = 0.15 + (random.nextDouble() * 0.1)
        val contactClickCount = (uniqueViewCount * contactClickRatio).toInt()
        
        // Saves: ~8-18% of views
        val savesRatio = 0.08 + (random.nextDouble() * 0.1)
        val savesCount = (uniqueViewCount * savesRatio).toInt()
        
        return ListingStatistics(
            uniqueViewCount = uniqueViewCount,
            inquiryCount = inquiryCount,
            messageCount = messageCount,
            contactClickCount = contactClickCount,
            savesCount = savesCount
        )
    }

    suspend fun generateSampleListings(
        landlordId: String,
        context: Context,
        titlePrefix: String? = null
    ): List<String> {
        if (landlordId.isBlank()) return emptyList()

        val db = FirebaseFirestore.getInstance()
        val createdIds = mutableListOf<String>()

        deleteExistingSampleListings(landlordId)

        sampleListings.forEachIndexed { index, template ->
            val docRef = db.collection("listings").document()
            val now = Timestamp.now()

            val finalTitle = titlePrefix?.let { "$it ${template.title}" } ?: template.title

            // --- IMAGE GENERATION ---
            val baseName = "myhouse" // change if needed
            val imageResIds = getListingImageResIds(context, baseName)

            val photoUrls = imageResIds.mapIndexed { i, resId ->
                val uri = resUri(context, resId)
                uploadCompressedImage(
                    context,
                    uri,
                    "listings/${docRef.id}/photo_$i.jpg"
                )
            }

            // Generate random statistics for the listing
            val stats = generateRandomStatistics(template.price, template.capacity, template.amenities.size)

            val listingMap = hashMapOf<String, Any>(
                "landlordId" to landlordId,
                "title" to finalTitle,
                "description" to template.description,
                "location" to template.location,
                "price" to template.price,
                "deposit" to template.deposit,
                "capacity" to template.capacity,
                // Required by Firestore rules for listing creation
                "genderPolicy" to "any",
                "currentOccupancy" to 0,
                "hasAvailableSlots" to true,
                "status" to template.status,
                "reviewStatus" to "under_review",
                "reviewedBy" to "",
                "reviewNotes" to "",
                "amenities" to template.amenities,
                "photos" to photoUrls,
                "createdAt" to now,
                "updatedAt" to now,
                "uniqueViewCount" to stats.uniqueViewCount,
                "inquiryCount" to stats.inquiryCount,
                "messageCount" to stats.messageCount,
                "contactClickCount" to stats.contactClickCount,
                "savesCount" to stats.savesCount,
                "isSample" to true,
                "reviewSummary" to mapOf(
                    "total" to 0,
                    "recommendedCount" to 0,
                    "notRecommendedCount" to 0,
                    "updatedAt" to now
                )
            )

            docRef.set(listingMap).await()
            createdIds.add(docRef.id)

            // Generate sample reviews
            val (recommendedCount, notRecommendedCount) = reviewCounts.getOrElse(index) { 0 to 0 }
            seedSampleReviewsForListing(
                listingId = docRef.id,
                recommendedCount = recommendedCount,
                notRecommendedCount = notRecommendedCount
            )
        }

        return createdIds
    }

    private fun getListingImageResIds(context: Context, baseName: String): List<Int> {
        return (1..5).mapNotNull { i ->
            val resName = "test_listing_${baseName}$i"
            val resId = context.resources.getIdentifier(
                resName,
                "drawable",
                context.packageName
            )
            if (resId != 0) resId else null
        }
    }

    private suspend fun deleteExistingSampleListings(landlordId: String) {
        val db = FirebaseFirestore.getInstance()

        val query = db.collection("listings")
            .whereEqualTo("landlordId", landlordId)
            .whereEqualTo("isSample", true)
            .get()
            .await()

        query.documents.forEach {
            it.reference.delete().await()
        }
    }

    private fun resUri(context: Context, resId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    private suspend fun seedSampleReviewsForListing(
        listingId: String,
        recommendedCount: Int,
        notRecommendedCount: Int
    ) {
        if (listingId.isBlank()) return
        val total = recommendedCount + notRecommendedCount
        if (total <= 0) return

        val db = FirebaseFirestore.getInstance()
        val reviewsRef = db.collection("listings").document(listingId).collection("reviews")
        val now = System.currentTimeMillis()

        for (i in 0 until recommendedCount) {
            val comment = positiveComments[i % positiveComments.size]
            val review = mapOf(
                "listingId" to listingId,
                "reviewerId" to "seed_tenant_${(i % 5) + 1}",
                "recommended" to true,
                "comment" to comment,
                "createdAt" to Timestamp(Date(now - (i + 1L) * 86_400_000L))
            )
            reviewsRef.add(review).await()
        }

        for (i in 0 until notRecommendedCount) {
            val comment = negativeComments[i % negativeComments.size]
            val review = mapOf(
                "listingId" to listingId,
                "reviewerId" to "seed_tenant_${(i % 5) + 1}",
                "recommended" to false,
                "comment" to comment,
                "createdAt" to Timestamp(Date(now - (i + 1L) * 86_400_000L))
            )
            reviewsRef.add(review).await()
        }

        val summary = mapOf(
            "total" to total,
            "recommendedCount" to recommendedCount,
            "notRecommendedCount" to notRecommendedCount,
            "updatedAt" to Timestamp.now()
        )
        db.collection("listings").document(listingId).update("reviewSummary", summary).await()
    }
}