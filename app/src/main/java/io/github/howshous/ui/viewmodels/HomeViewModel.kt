package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.RentalRepository
import io.github.howshous.data.firestore.IssueRepository
import io.github.howshous.data.models.Listing
import io.github.howshous.data.models.Rental
import io.github.howshous.data.models.Issue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val listingRepo = ListingRepository()
    private val rentalRepo = RentalRepository()
    private val issueRepo = IssueRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // For Tenant
    private val _tenantRecentListings = MutableStateFlow<List<Listing>>(emptyList())
    val tenantRecentListings: StateFlow<List<Listing>> = _tenantRecentListings

    private val _tenantRentals = MutableStateFlow<List<Rental>>(emptyList())
    val tenantRentals: StateFlow<List<Rental>> = _tenantRentals

    // For Landlord
    private val _landlordListings = MutableStateFlow<List<Listing>>(emptyList())
    val landlordListings: StateFlow<List<Listing>> = _landlordListings

    private val _landlordRentals = MutableStateFlow<List<Rental>>(emptyList())
    val landlordRentals: StateFlow<List<Rental>> = _landlordRentals

    // KPI data
    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount

    private val _vacantCount = MutableStateFlow(0)
    val vacantCount: StateFlow<Int> = _vacantCount

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount

    // Issues for landlord
    private val _landlordIssues = MutableStateFlow<List<Issue>>(emptyList())
    val landlordIssues: StateFlow<List<Issue>> = _landlordIssues

    fun loadTenantHome(tenantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val listings = listingRepo.getAllListings().take(5)
            val rentals = rentalRepo.getRentalsForTenant(tenantId)
            _tenantRecentListings.value = listings
            _tenantRentals.value = rentals
            _isLoading.value = false
        }
    }

    fun loadLandlordHome(landlordId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val listings = listingRepo.getListingsForLandlord(landlordId)
            val rentals = rentalRepo.getRentalsForLandlord(landlordId)

            _landlordListings.value = listings
            _landlordRentals.value = rentals

            // Calculate KPIs
            val activeListings = listings.count { it.status == "active" }
            val vacantListings = listings.count { it.status == "active" && rentals.none { r -> r.listingId == it.id } }
            val overdueRentals = rentals.count { it.status == "overdue" }

            _activeCount.value = activeListings
            _vacantCount.value = vacantListings
            _overdueCount.value = overdueRentals

            // Load recent issues
            val issues = issueRepo.getIssuesForLandlord(landlordId, 10)
            _landlordIssues.value = issues

            _isLoading.value = false
        }
    }
}
