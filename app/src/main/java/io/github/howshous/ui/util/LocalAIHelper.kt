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
        
        val queryLower = query.lowercase().trim()
        
        // Handle greetings and casual queries
        if (isGreeting(queryLower)) {
            return generateGreetingResponse(listings)
        }
        
        // Handle comparison requests
        if (isComparisonRequest(queryLower)) {
            return generateComparisonResponse(queryLower, listings)
        }
        
        // Handle specific question types
        val questionType = detectQuestionType(queryLower)
        
        // Extract user preferences with better parsing
        val budget = extractBudget(queryLower)
        val locationPref = extractLocation(queryLower)
        val amenitiesPref = extractAmenities(queryLower)
        val keywords = extractKeywords(queryLower)
        
        // Filter and score listings with smarter algorithm
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
            } else {
                // If no budget specified, prefer mid-range options
                val avgPrice = listings.map { it.price }.average()
                val priceDiff = kotlin.math.abs(listing.price - avgPrice)
                score += 20.0 - (priceDiff / avgPrice) * 10.0
            }
            
            // Location matching (exact match gets higher score)
            if (locationPref.isNotEmpty()) {
                val listingLoc = listing.location.lowercase()
                when {
                    listingLoc.contains(locationPref) -> score += 30.0
                    listingLoc.split(" ").any { it.contains(locationPref) || locationPref.contains(it) } -> score += 15.0
                }
            }
            
            // Amenities matching (more matches = higher score)
            val matchedAmenities = amenitiesPref.count { amenity ->
                listing.amenities.any { it.lowercase().contains(amenity) || amenity.contains(it.lowercase()) }
            }
            score += matchedAmenities * 15.0
            
            // Keyword matching in title, description
            val titleDesc = "${listing.title} ${listing.description}".lowercase()
            keywords.forEach { keyword ->
                if (titleDesc.contains(keyword)) {
                    score += 10.0
                }
            }
            
            // Description relevance
            if (listing.description.isNotEmpty()) {
                val descLower = listing.description.lowercase()
                if (keywords.any { descLower.contains(it) }) {
                    score += 5.0
                }
            }
            
            // Base score for all listings
            score += 10.0
            
            Pair(listing, score)
        }.sortedByDescending { it.second }
        
        // Get top recommendations (more if comparison requested)
        val topCount = if (questionType == QuestionType.COMPARE) 5 else 3
        val topListings = scoredListings.take(topCount).filter { it.second > 0 }
        
        if (topListings.isEmpty()) {
            // No good matches, suggest cheapest or most relevant
            val cheapest = listings.minByOrNull { it.price }
            return if (cheapest != null) {
                buildNoMatchResponse(query, cheapest, budget, locationPref)
            } else {
                "I couldn't find any listings that match your criteria. Could you try adjusting your budget or preferences?"
            }
        }
        
        return buildRecommendationResponse(query, topListings, budget, locationPref, questionType)
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
    
    private fun extractKeywords(query: String): List<String> {
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "i", "want", "need", "looking", "for", "find", "show", "me", "have", "is", "are", "what", "where", "how", "much", "does", "cost", "price", "budget", "can", "you", "help", "please")
        return query.split(" ")
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }
    
    private fun isGreeting(query: String): Boolean {
        val greetings = listOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening", "greetings", "what's up", "how are you")
        return greetings.any { query.contains(it) }
    }
    
    private fun isComparisonRequest(query: String): Boolean {
        val comparisonWords = listOf("compare", "difference", "which is better", "what's the difference", "vs", "versus", "between")
        return comparisonWords.any { query.contains(it) }
    }
    
    private enum class QuestionType {
        BUDGET, LOCATION, AMENITIES, COMPARE, GENERAL, CHEAPEST, BEST
    }
    
    private fun detectQuestionType(query: String): QuestionType {
        return when {
            query.contains("cheapest") || query.contains("lowest price") || query.contains("most affordable") -> QuestionType.CHEAPEST
            query.contains("best") || query.contains("recommend") || query.contains("suggest") -> QuestionType.BEST
            query.contains("compare") || query.contains("difference") || query.contains("vs") -> QuestionType.COMPARE
            query.contains("budget") || query.contains("price") || query.contains("cost") || query.contains("afford") -> QuestionType.BUDGET
            query.contains("where") || query.contains("location") || query.contains("near") -> QuestionType.LOCATION
            query.contains("wifi") || query.contains("amenities") || query.contains("features") -> QuestionType.AMENITIES
            else -> QuestionType.GENERAL
        }
    }
    
    private fun generateGreetingResponse(listings: List<ListingData>): String {
        val count = listings.size
        val greetings = listOf(
            "Hello! I'm here to help you find the perfect place to stay in Baguio!",
            "Hi there! I'd be happy to help you find a great place to call home.",
            "Hey! Ready to find your ideal boarding house? Let's get started!"
        )
        val randomGreeting = greetings.random()
        return "$randomGreeting\n\nI currently have **$count** ${if (count == 1) "listing" else "listings"} available. " +
                "You can ask me about:\n" +
                "• Budget-friendly options\n" +
                "• Specific locations\n" +
                "• Amenities you need\n" +
                "• Comparisons between listings\n\n" +
                "What are you looking for?"
    }
    
    private fun generateComparisonResponse(query: String, listings: List<ListingData>): String {
        // Try to extract listing titles or IDs from query
        val queryWords = query.split(" ").map { it.lowercase() }
        val matchedListings = listings.filter { listing ->
            queryWords.any { word ->
                listing.title.lowercase().contains(word) || 
                listing.location.lowercase().contains(word) ||
                word.length > 3 && listing.description.lowercase().contains(word)
            }
        }.take(5)
        
        if (matchedListings.size < 2) {
            return "I'd be happy to help you compare listings! Could you tell me which specific places you'd like to compare? " +
                    "You can mention them by name, location, or price range."
        }
        
        val response = StringBuilder()
        response.append("**Here's a comparison of the listings you mentioned:**\n\n")
        
        matchedListings.forEachIndexed { index, listing ->
            response.append("**${index + 1}. ${listing.title}**\n")
            response.append("• **Price:** ₱${listing.price}/month\n")
            response.append("• **Location:** ${listing.location}\n")
            if (listing.amenities.isNotEmpty()) {
                response.append("• **Amenities:** ${listing.amenities.take(5).joinToString(", ")}${if (listing.amenities.size > 5) "..." else ""}\n")
            }
            if (listing.description.isNotEmpty()) {
                response.append("• **Highlights:** ${listing.description.take(100)}${if (listing.description.length > 100) "..." else ""}\n")
            }
            response.append("\n")
        }
        
        response.append("Would you like more details about any of these?")
        return response.toString()
    }
    
    private fun buildRecommendationResponse(
        query: String,
        topListings: List<Pair<ListingData, Double>>,
        budget: Int,
        locationPref: String,
        questionType: QuestionType
    ): String {
        val response = StringBuilder()
        
        // Add contextual opening based on query type
        when (questionType) {
            QuestionType.CHEAPEST -> response.append("**Here are the most affordable options I found:**\n\n")
            QuestionType.BEST -> response.append("**Based on your preferences, here are my top recommendations:**\n\n")
            QuestionType.LOCATION -> {
                if (locationPref.isNotEmpty()) {
                    response.append("**Great! I found some options ${if (locationPref.contains("near") || locationPref.contains("close")) "" else "in "}$locationPref:**\n\n")
                } else {
                    response.append("**Here are some great options:**\n\n")
                }
            }
            QuestionType.BUDGET -> {
                if (budget > 0) {
                    response.append("**Here are options within your budget of ₱$budget:**\n\n")
                } else {
                    response.append("**Here are some great options:**\n\n")
                }
            }
            else -> {
                if (topListings.size == 1) {
                    response.append("**Perfect Match!**\n\n")
                } else {
                    response.append("**Here are my top ${topListings.size} recommendations:**\n\n")
                }
            }
        }
        
        if (topListings.size == 1) {
            val (listing, score) = topListings[0]
            response.append("**${listing.title}**\n")
            response.append("• **Price:** ₱${listing.price} per month\n")
            response.append("• **Location:** ${listing.location}\n")
            if (listing.amenities.isNotEmpty()) {
                val topAmenities = listing.amenities.take(5)
                response.append("• **Key Features:** ${topAmenities.joinToString(", ")}${if (listing.amenities.size > 5) "..." else ""}\n")
            }
            if (listing.description.isNotEmpty()) {
                response.append("• **About:** ${listing.description.take(200)}${if (listing.description.length > 200) "..." else ""}\n")
            }
            if (budget > 0 && listing.price > budget) {
                val diff = listing.price - budget
                response.append("\n*Note: This is ₱$diff above your budget, but it's the closest match I found and offers great value.*")
            } else if (budget > 0 && listing.price <= budget) {
                val savings = budget - listing.price
                response.append("\n*Great news! This fits perfectly within your budget with ₱$savings to spare.*")
            }
        } else {
            topListings.forEachIndexed { index, (listing, score) ->
                response.append("**${index + 1}. ${listing.title}**\n")
                response.append("• **Price:** ₱${listing.price}/month")
                if (budget > 0) {
                    val diff = listing.price - budget
                    when {
                        diff < 0 -> response.append(" (₱${-diff} under budget)")
                        diff == 0 -> response.append(" (perfect match!)")
                        diff <= budget * 0.2 -> response.append(" (₱$diff over, but worth it)")
                    }
                }
                response.append("\n")
                response.append("• **Location:** ${listing.location}\n")
                if (listing.amenities.isNotEmpty()) {
                    val topAmenities = listing.amenities.take(4)
                    response.append("• **Features:** ${topAmenities.joinToString(", ")}${if (listing.amenities.size > 4) "..." else ""}\n")
                }
                if (listing.description.isNotEmpty() && index == 0) {
                    // Add description for top recommendation
                    response.append("• **Why this one:** ${listing.description.take(120)}${if (listing.description.length > 120) "..." else ""}\n")
                }
                response.append("\n")
            }
            
            // Add contextual closing
            val closings = listOf(
                "Would you like more details about any of these?",
                "Which one interests you most? I can provide more information!",
                "Feel free to ask me anything about these listings!"
            )
            response.append(closings.random())
        }
        
        return response.toString()
    }
    
    private fun buildNoMatchResponse(
        query: String, 
        cheapest: ListingData, 
        budget: Int, 
        locationPref: String
    ): String {
        return buildString {
            append("I couldn't find any listings that perfectly match your criteria")
            if (budget > 0 || locationPref.isNotEmpty()) {
                append(" (")
                val conditions = mutableListOf<String>()
                if (budget > 0) conditions.add("budget: ₱$budget")
                if (locationPref.isNotEmpty()) conditions.add("location: $locationPref")
                append(conditions.joinToString(", "))
                append(")")
            }
            append(".\n\n")
            
            append("However, here's the most affordable option I found:\n\n")
            append("**${cheapest.title}**\n")
            append("• **Price:** ₱${cheapest.price} per month\n")
            append("• **Location:** ${cheapest.location}\n")
            if (cheapest.amenities.isNotEmpty()) {
                append("• **Features:** ${cheapest.amenities.take(5).joinToString(", ")}${if (cheapest.amenities.size > 5) "..." else ""}\n")
            }
            if (cheapest.description.isNotEmpty()) {
                append("• **About:** ${cheapest.description.take(150)}${if (cheapest.description.length > 150) "..." else ""}\n")
            }
            
            if (budget > 0 && cheapest.price > budget) {
                val diff = cheapest.price - budget
                append("\n*This is ₱$diff above your budget, but it's the best option available. ")
                append("Would you like to see more details, or should I look for other options?*")
            } else {
                append("\n*Would you like more details about this, or would you prefer to adjust your search criteria?*")
            }
        }
    }
}


