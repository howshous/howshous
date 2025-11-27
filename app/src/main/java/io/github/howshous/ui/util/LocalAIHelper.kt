package io.github.howshous.ui.util

import org.json.JSONArray
import org.json.JSONObject

data class ListingData(
    val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val description: String,
    val amenities: List<String>
)

object LocalAIHelper {
    
    fun generateRecommendation(query: String, listingsJson: String): String {
        val listings = parseListings(listingsJson)
        if (listings.isEmpty()) {
            return "I don't have any available listings in the database right now. Please check back later!"
        }
        
        val queryLower = query.lowercase()
        
        // Extract user preferences
        val budget = extractBudget(queryLower)
        val locationPref = extractLocation(queryLower)
        val amenitiesPref = extractAmenities(queryLower)
        
        // Filter and score listings
        val scoredListings = listings.map { listing ->
            var score = 0.0
            
            // Budget matching (closer to budget = higher score, but allow slightly over)
            if (budget > 0) {
                val priceDiff = listing.price - budget
                when {
                    listing.price <= budget -> score += 50.0 // Perfect match
                    priceDiff <= budget * 0.2 -> score += 30.0 - (priceDiff / budget) * 20.0 // Within 20% over
                    priceDiff <= budget * 0.5 -> score += 10.0 - (priceDiff / budget) * 10.0 // Within 50% over
                    else -> score -= 20.0 // Too expensive
                }
            }
            
            // Location matching
            if (locationPref.isNotEmpty()) {
                if (listing.location.lowercase().contains(locationPref)) {
                    score += 30.0
                }
            }
            
            // Amenities matching
            amenitiesPref.forEach { amenity ->
                if (listing.amenities.any { it.lowercase().contains(amenity) }) {
                    score += 10.0
                }
            }
            
            // Base score for all listings
            score += 10.0
            
            Pair(listing, score)
        }.sortedByDescending { it.second }
        
        // Get top recommendations
        val topListings = scoredListings.take(3).filter { it.second > 0 }
        
        if (topListings.isEmpty()) {
            // No good matches, suggest cheapest
            val cheapest = listings.minByOrNull { it.price }
            return if (cheapest != null) {
                buildNoMatchResponse(query, cheapest)
            } else {
                "I couldn't find any listings that match your criteria. Could you try adjusting your budget or preferences?"
            }
        }
        
        return buildRecommendationResponse(query, topListings, budget)
    }
    
    private fun parseListings(jsonString: String): List<ListingData> {
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).mapNotNull { i ->
                val obj = jsonArray.getJSONObject(i)
                ListingData(
                    id = obj.optString("id", ""),
                    title = obj.optString("title", ""),
                    price = obj.optInt("price", 0),
                    location = obj.optString("location", ""),
                    description = obj.optString("description", ""),
                    amenities = (obj.optJSONArray("amenities")?.let { arr ->
                        (0 until arr.length()).mapNotNull { arr.optString(it, null) }
                    } ?: emptyList())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun extractBudget(query: String): Int {
        val patterns = listOf(
            Regex("""(?:budget|price|cost|rent|pay|afford)\s*(?:is|of|around|about|up to|max|maximum)?\s*\$?(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""\$(\d+)"""),
            Regex("""(\d+)\s*(?:pesos|php|per month|monthly)""", RegexOption.IGNORE_CASE)
        )
        
        patterns.forEach { pattern ->
            pattern.find(query)?.groupValues?.get(1)?.toIntOrNull()?.let {
                return it
            }
        }
        return 0
    }
    
    private fun extractLocation(query: String): String {
        val baguioLocations = listOf(
            "session road", "burnham park", "camp john hay", "mines view",
            "wright park", "legarda", "leonard wood", "naguilian",
            "marcos highway", "loakan", "asin", "irisan", "aurora hill",
            "quezon hill", "pinsao", "bakakeng", "trancoville",
            "magsaysay", "pacdal", "lualhati", "happy hollow",
            "dagsian", "crystal cave", "lucban", "gibraltar",
            "teodoro alonzo", "rizal monument", "baguio city center",
            "baguio", "city center"
        )
        
        baguioLocations.forEach { location ->
            if (query.contains(location, ignoreCase = true)) {
                return location
            }
        }
        return ""
    }
    
    private fun extractAmenities(query: String): List<String> {
        val amenities = listOf(
            "wifi", "internet", "aircon", "air conditioning", "parking",
            "kitchen", "laundry", "gym", "pool", "security", "cctv",
            "furnished", "unfurnished", "near", "close to", "walking distance"
        )
        
        return amenities.filter { query.contains(it, ignoreCase = true) }
    }
    
    private fun buildRecommendationResponse(
        query: String,
        topListings: List<Pair<ListingData, Double>>,
        budget: Int
    ): String {
        val response = StringBuilder()
        
        if (topListings.size == 1) {
            val (listing, _) = topListings[0]
            response.append("**Perfect Match!**\n\n")
            response.append("I found a great option for you:\n\n")
            response.append("**${listing.title}**\n")
            response.append("• **Price:** ₱${listing.price} per month\n")
            response.append("• **Location:** ${listing.location}\n")
            if (listing.amenities.isNotEmpty()) {
                response.append("• **Amenities:** ${listing.amenities.joinToString(", ")}\n")
            }
            if (listing.description.isNotEmpty()) {
                response.append("• **Description:** ${listing.description.take(150)}${if (listing.description.length > 150) "..." else ""}\n")
            }
            if (budget > 0 && listing.price > budget) {
                response.append("\n*Note: This is slightly above your budget, but it's the closest match I found.*")
            }
        } else {
            response.append("**Here are my top ${topListings.size} recommendations:**\n\n")
            
            topListings.forEachIndexed { index, (listing, score) ->
                response.append("**${index + 1}. ${listing.title}**\n")
                response.append("• **Price:** ₱${listing.price} per month\n")
                response.append("• **Location:** ${listing.location}\n")
                if (listing.amenities.isNotEmpty()) {
                    response.append("• **Amenities:** ${listing.amenities.joinToString(", ")}\n")
                }
                response.append("\n")
            }
            
            response.append("Would you like more details about any of these?")
        }
        
        return response.toString()
    }
    
    private fun buildNoMatchResponse(query: String, cheapest: ListingData): String {
        return buildString {
            append("I couldn't find any listings that perfectly match your criteria.\n\n")
            append("However, here's the most affordable option I found:\n\n")
            append("**${cheapest.title}**\n")
            append("• **Price:** ₱${cheapest.price} per month\n")
            append("• **Location:** ${cheapest.location}\n")
            if (cheapest.amenities.isNotEmpty()) {
                append("• **Amenities:** ${cheapest.amenities.joinToString(", ")}\n")
            }
            append("\n*This might be slightly above your budget, but it's the best option available. Would you like to see more details?*")
        }
    }
}

