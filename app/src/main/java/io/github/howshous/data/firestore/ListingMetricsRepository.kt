package io.github.howshous.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Per-listing aggregated metrics (7d and 30d) read from listing_daily_stats.
 * Landlords can read only their own listing stats via Firestore rules.
 */
data class ListingMetrics(
    val listingId: String,
    val views7d: Int,
    val uniqueSessions7d: Int,
    val saves7d: Int,
    val messages7d: Int,
    val views30d: Int,
    val uniqueSessions30d: Int,
    val saves30d: Int,
    val messages30d: Int,
)

class ListingMetricsRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.add(java.util.Calendar.DAY_OF_MONTH, -daysAgo)
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private fun isWithinLastNDays(dateStr: String, days: Int): Boolean {
        val parts = dateStr.split("-")
        if (parts.size != 3) return false
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val then = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(parts[0].toIntOrNull() ?: return false, (parts[1].toIntOrNull() ?: return false) - 1, parts[2].toIntOrNull() ?: return false)
        }
        val diffMs = cal.timeInMillis - then.timeInMillis
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        return diffDays in 0..days
    }

    suspend fun getMetricsForListing(listingId: String): ListingMetrics? {
        if (listingId.isBlank()) return null
        return try {
            val thirtyDaysAgo = dateKeyDaysAgo(30)
            val snap = db.collection("listing_daily_stats")
                .document(listingId)
                .collection("days")
                .whereGreaterThanOrEqualTo("date", thirtyDaysAgo)
                .get()
                .await()

            var views7d = 0
            var uniqueSessions7d = 0
            var saves7d = 0
            var messages7d = 0
            var views30d = 0
            var uniqueSessions30d = 0
            var saves30d = 0
            var messages30d = 0

            for (doc in snap.documents) {
                val date = doc.getString("date") ?: continue
                val views = (doc.get("views") as? Number)?.toInt() ?: 0
                val uniqueSessions = (doc.get("uniqueSessions") as? Number)?.toInt() ?: 0
                val saves = (doc.get("saves") as? Number)?.toInt() ?: 0
                val messages = (doc.get("messages") as? Number)?.toInt() ?: 0

                if (isWithinLastNDays(date, 30)) {
                    views30d += views
                    uniqueSessions30d += uniqueSessions
                    saves30d += saves
                    messages30d += messages
                }
                if (isWithinLastNDays(date, 7)) {
                    views7d += views
                    uniqueSessions7d += uniqueSessions
                    saves7d += saves
                    messages7d += messages
                }
            }

            ListingMetrics(
                listingId = listingId,
                views7d = views7d,
                uniqueSessions7d = uniqueSessions7d,
                saves7d = saves7d,
                messages7d = messages7d,
                views30d = views30d,
                uniqueSessions30d = uniqueSessions30d,
                saves30d = saves30d,
                messages30d = messages30d,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMetricsForListings(listingIds: List<String>): Map<String, ListingMetrics> {
        if (listingIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, ListingMetrics>()
        for (id in listingIds) {
            getMetricsForListing(id)?.let { result[id] = it }
        }
        return result
    }
}
