package io.github.howshous.data.models

/**
 * Central definition of allowed analytics event types.
 * All analytics logging (client + backend) should use ONLY these values.
 */
enum class AnalyticsEventType(val value: String) {
    LISTING_VIEW("LISTING_VIEW"),
    LISTING_SAVE("LISTING_SAVE"),
    LISTING_MESSAGE("LISTING_MESSAGE"),
    SEARCH_PERFORMED("SEARCH_PERFORMED");
}

/**
 * Whitelisted search filter keys used for analytics.
 *
 * Amenities are dynamic labels but they are always encoded using the
 * "amenity:{label}" convention.
 */
enum class SearchFilterKey(val value: String) {
    QUERY("query"),
    MIN_PRICE("minPrice"),
    MAX_PRICE("maxPrice");

    companion object {
        fun amenityKey(label: String): String = "amenity:$label"
    }
}

