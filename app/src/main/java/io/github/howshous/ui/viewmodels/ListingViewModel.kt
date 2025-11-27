package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.models.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TenantSearchViewModel : ViewModel() {
    private val listingRepo = ListingRepository()

    private val _allListings = MutableStateFlow<List<Listing>>(emptyList())
    val allListings: StateFlow<List<Listing>> = _allListings

    private val _filteredListings = MutableStateFlow<List<Listing>>(emptyList())
    val filteredListings: StateFlow<List<Listing>> = _filteredListings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        loadAllListings()
    }

    fun loadAllListings() {
        viewModelScope.launch {
            _isLoading.value = true
            val listings = listingRepo.getAllListings()
            _allListings.value = listings
            _filteredListings.value = listings
            _isLoading.value = false
        }
    }

    fun searchByLocation(location: String) {
        _searchQuery.value = location
        viewModelScope.launch {
            _isLoading.value = true
            val listings = if (location.isBlank()) {
                _allListings.value
            } else {
                _allListings.value.filter {
                    it.location.contains(location, ignoreCase = true) ||
                    it.title.contains(location, ignoreCase = true)
                }
            }
            _filteredListings.value = listings
            _isLoading.value = false
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

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
}
