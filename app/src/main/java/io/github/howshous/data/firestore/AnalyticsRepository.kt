package io.github.howshous.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.howshous.data.models.AnalyticsEventType
import io.github.howshous.data.models.SearchFilterKey
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

/**
 * Centralized analytics event logger used by the client.
 *
 * Responsibilities:
 * - Validate and normalize event types using AnalyticsEventType
 * - Attach session_id (when provided)
 * - Attach a server timestamp
 * - Write to the Firestore `analytics_events` collection
 */
class AnalyticsRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun eventsCollection() = db.collection("events")

    /** Today's date in UTC as YYYY-MM-DD (matches Cloud Function and ListingMetricsRepository). */
    private fun todayUtcDateKey(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private suspend fun logEvent(
        type: AnalyticsEventType,
        userId: String?,
        sessionId: String?,
        payload: Map<String, Any?>,
    ) {
        val data = mutableMapOf<String, Any?>(
            "eventType" to type.value,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        if (!userId.isNullOrBlank()) {
            data["userId"] = userId
        }
        if (!sessionId.isNullOrBlank()) {
            data["sessionId"] = sessionId
        }

        payload.forEach { (key, value) ->
            if (value != null) {
                data[key] = value
            }
        }

        runCatching { eventsCollection().add(data).await() }
    }

    suspend fun logListingView(
        listingId: String,
        landlordId: String,
        userId: String,
        sessionId: String?,
        price: Int?,
    ) {
        if (listingId.isBlank() || landlordId.isBlank() || userId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_VIEW,
            userId = userId,
            sessionId = sessionId,
            payload = mapOf(
                "listingId" to listingId,
                "landlordId" to landlordId,
                "price" to price,
            ),
        )
    }

    suspend fun logSearchFilters(
        userId: String,
        sessionId: String?,
        hasQuery: Boolean,
        minPrice: Int?,
        maxPrice: Int?,
        amenities: Set<String>,
    ) {
        if (userId.isBlank() && sessionId.isNullOrBlank()) return

        val activeFilterKeys = mutableListOf<String>()
        if (hasQuery) activeFilterKeys += SearchFilterKey.QUERY.value
        if (minPrice != null) activeFilterKeys += SearchFilterKey.MIN_PRICE.value
        if (maxPrice != null) activeFilterKeys += SearchFilterKey.MAX_PRICE.value
        amenities.forEach { amenity ->
            activeFilterKeys += SearchFilterKey.amenityKey(amenity)
        }

        logEvent(
            type = AnalyticsEventType.SEARCH_PERFORMED,
            userId = userId.ifBlank { null },
            sessionId = sessionId,
            payload = mapOf(
                // filter keys (no free-text query)
                "filterKeys" to activeFilterKeys,
                // numeric context for analysis
                "minPrice" to (minPrice ?: 0),
                "maxPrice" to (maxPrice ?: 0),
                "amenities" to amenities.toList(),
            ),
        )
    }

    suspend fun logMessageSent(
        chatId: String,
        listingId: String,
        landlordId: String,
        senderId: String,
    ) {
        if (chatId.isBlank() || listingId.isBlank() || landlordId.isBlank() || senderId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_MESSAGE,
            userId = senderId,
            sessionId = null,
            payload = mapOf(
                "chatId" to chatId,
                "listingId" to listingId,
                "landlordId" to landlordId,
            ),
        )
    }

    suspend fun logListingSave(
        listingId: String,
        landlordId: String,
        userId: String,
        sessionId: String?,
        price: Int?,
    ) {
        if (listingId.isBlank() || landlordId.isBlank() || userId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_SAVE,
            userId = userId,
            sessionId = sessionId,
            payload = mapOf(
                "listingId" to listingId,
                "landlordId" to landlordId,
                "price" to price,
            ),
        )
    }

    /**
     * Writes synthetic analytics events for the given landlord's listings so that
     * listing_daily_stats and metrics can be tested. Callable from landlord UI only.
     * Uses fake but consistent userIds/sessionIds so aggregates are predictable.
     */
    suspend fun seedTestEventsForLandlord(
        landlordId: String,
        listings: List<Pair<String, Int>>,
    ) {
        if (landlordId.isBlank() || listings.isEmpty()) return

        val seedUsers = listOf("seed_tenant_1", "seed_tenant_2", "seed_tenant_3")
        val seedSessions = listOf("seed_session_1", "seed_session_2", "seed_session_3", "seed_session_4")

        for ((listingId, price) in listings) {
            if (listingId.isBlank()) continue

            // 4 unique session views per listing (so views + uniqueSessions increment)
            for (i in seedSessions.indices) {
                logListingView(
                    listingId = listingId,
                    landlordId = landlordId,
                    userId = seedUsers[i % seedUsers.size],
                    sessionId = seedSessions[i],
                    price = price,
                )
            }

            // 2 saves per listing (different users)
            logListingSave(listingId, landlordId, seedUsers[0], seedSessions[0], price)
            logListingSave(listingId, landlordId, seedUsers[1], seedSessions[1], price)

            // 1 first-message per listing (one chat)
            logMessageSent(
                chatId = "seed_chat_$listingId",
                listingId = listingId,
                landlordId = landlordId,
                senderId = seedUsers[0],
            )
        }

        // 3 search events (filter usage for search_metrics)
        logSearchFilters(seedUsers[0], seedSessions[0], hasQuery = true, 5000, 15000, setOf("WiFi", "Air Conditioning"))
        logSearchFilters(seedUsers[1], seedSessions[1], hasQuery = true, null, 10000, setOf("Free Parking"))
        logSearchFilters(seedUsers[2], seedSessions[2], hasQuery = false, 3000, null, setOf("WiFi", "Laundry"))

        // Write aggregated daily stats so the app shows metrics without needing the Cloud Function
        // (e.g. when using the emulator without Functions). Matches Cloud Function doc shape.
        val dateKey = todayUtcDateKey()
        val dailyStats = db.collection("listing_daily_stats")
        for ((listingId, _) in listings) {
            if (listingId.isBlank()) continue
            val dayRef = dailyStats.document(listingId).collection("days").document(dateKey)
            val dayData = mapOf(
                "listingId" to listingId,
                "landlordId" to landlordId,
                "date" to dateKey,
                "views" to 4,
                "uniqueSessions" to 4,
                "saves" to 2,
                "messages" to 1,
            )
            runCatching { dayRef.set(dayData, SetOptions.merge()).await() }
        }
    }
}

