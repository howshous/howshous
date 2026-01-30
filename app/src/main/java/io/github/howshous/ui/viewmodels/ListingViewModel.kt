package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.AnalyticsRepository
import io.github.howshous.data.firestore.SavedListingsRepository
import io.github.howshous.data.models.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TenantSearchViewModel : ViewModel() {
    private val listingRepo = ListingRepository()
    private val analyticsRepo = AnalyticsRepository()

    private val _allListings = MutableStateFlow<List<Listing>>(emptyList())
    val allListings: StateFlow<List<Listing>> = _allListings

    private val _filteredListings = MutableStateFlow<List<Listing>>(emptyList())
    val filteredListings: StateFlow<List<Listing>> = _filteredListings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _minPriceInput = MutableStateFlow("")
    val minPriceInput: StateFlow<String> = _minPriceInput

    private val _maxPriceInput = MutableStateFlow("")
    val maxPriceInput: StateFlow<String> = _maxPriceInput

    private val _selectedAmenities = MutableStateFlow<Set<String>>(emptySet())
    val selectedAmenities: StateFlow<Set<String>> = _selectedAmenities

    init {
        loadAllListings()
    }

    fun loadAllListings() {
        viewModelScope.launch {
            _isLoading.value = true
            val listings = listingRepo.getAllListings()
            _allListings.value = listings
            applyFilters()
            _isLoading.value = false
        }
    }

    fun searchByLocation(location: String) {
        _searchQuery.value = location
        applyFilters()
    }

    fun updateMinPriceInput(value: String) {
        _minPriceInput.value = value.filter { it.isDigit() }
        applyFilters()
    }

    fun updateMaxPriceInput(value: String) {
        _maxPriceInput.value = value.filter { it.isDigit() }
        applyFilters()
    }

    fun toggleAmenity(amenity: String) {
        _selectedAmenities.value = if (_selectedAmenities.value.contains(amenity)) {
            _selectedAmenities.value - amenity
        } else {
            _selectedAmenities.value + amenity
        }
        applyFilters()
    }

    fun clearFilters() {
        _minPriceInput.value = ""
        _maxPriceInput.value = ""
        _selectedAmenities.value = emptySet()
        applyFilters()
    }

    private fun applyFilters() {
        _isLoading.value = true
        val query = _searchQuery.value.trim()
        val minPrice = _minPriceInput.value.toIntOrNull()
        val maxPrice = _maxPriceInput.value.toIntOrNull()
        val amenities = _selectedAmenities.value

        val listings = _allListings.value.filter { listing ->
            val matchesQuery = query.isBlank() ||
                listing.location.contains(query, ignoreCase = true) ||
                listing.title.contains(query, ignoreCase = true)
            val matchesMin = minPrice == null || listing.price >= minPrice
            val matchesMax = maxPrice == null || listing.price <= maxPrice
            val matchesAmenities = amenities.isEmpty() || amenities.all { listing.amenities.contains(it) }

            matchesQuery && matchesMin && matchesMax && matchesAmenities
        }

        _filteredListings.value = listings
        _isLoading.value = false
    }

    fun logCurrentFilters(
        userId: String,
        sessionId: String?
    ) {
        viewModelScope.launch {
            val query = _searchQuery.value.trim()
            val minPrice = _minPriceInput.value.toIntOrNull()
            val maxPrice = _maxPriceInput.value.toIntOrNull()
            val amenities = _selectedAmenities.value

            analyticsRepo.logSearchFilters(
                userId = userId,
                sessionId = sessionId,
                hasQuery = query.isNotEmpty(),
                minPrice = minPrice,
                maxPrice = maxPrice,
                amenities = amenities
            )
        }
    }
}

class LandlordListingsViewModel : ViewModel() {
    private val listingRepo = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadListingsForLandlord(landlordId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val listings = listingRepo.getListingsForLandlord(landlordId)
            _listings.value = listings
            _isLoading.value = false
        }
    }

    fun createNewListing(listing: Listing) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = listingRepo.createListing(listing)
            if (id.isNotEmpty()) {
                loadListingsForLandlord(listing.landlordId)
            }
            _isLoading.value = false
        }
    }
}

class ListingViewModel : ViewModel() {
    private val listingRepo = ListingRepository()
    private val analyticsRepo = AnalyticsRepository()
    private val savedRepo = SavedListingsRepository()

    // Tracks which listings have already been logged as viewed in this ViewModel's lifetime
    private val viewedInSession: MutableSet<String> = mutableSetOf()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    fun loadListing(listingId: String) {
        if (listingId.isBlank()) {
            _listing.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = listingRepo.getListing(listingId)
                _listing.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _listing.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSavedState(listingId: String, userId: String) {
        if (listingId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            _isSaved.value = savedRepo.isListingSaved(userId, listingId)
        }
    }

    fun toggleSave(
        listingId: String,
        userId: String,
        sessionId: String?
    ) {
        val current = _listing.value
        if (listingId.isBlank() || userId.isBlank() || current == null) return

        viewModelScope.launch {
            val nowSaved = !_isSaved.value
            if (nowSaved) {
                savedRepo.saveListing(userId, listingId, current.price)
                analyticsRepo.logListingSave(
                    listingId = listingId,
                    landlordId = current.landlordId,
                    userId = userId,
                    sessionId = sessionId,
                    price = current.price
                )
            } else {
                savedRepo.unsaveListing(userId, listingId)
            }
            _isSaved.value = nowSaved
        }
    }

    fun recordUniqueViewWithAnalytics(
        listingId: String,
        viewerId: String,
        sessionId: String?,
        price: Int?
    ) {
        if (listingId.isBlank() || viewerId.isBlank()) return
        if (viewedInSession.contains(listingId)) return

        val current = _listing.value
        if (current == null || current.id != listingId) return

        viewModelScope.launch {
            listingRepo.recordUniqueView(listingId, viewerId)
            analyticsRepo.logListingView(
                listingId = listingId,
                landlordId = current.landlordId,
                userId = viewerId,
                sessionId = sessionId,
                price = price
            )
            viewedInSession.add(listingId)
        }
    }
}
