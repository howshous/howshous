package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ContractRepository
import io.github.howshous.data.models.Contract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContractsViewModel : ViewModel() {
    private val contractRepo = ContractRepository()

    private val _signedContracts = MutableStateFlow<List<Contract>>(emptyList())
    val signedContracts: StateFlow<List<Contract>> = _signedContracts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadSignedContracts(tenantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val contracts = contractRepo.getSignedContractsForTenant(tenantId)
            _signedContracts.value = contracts
            _isLoading.value = false
        }
    }

    suspend fun hasSignedContract(tenantId: String): Boolean {
        return contractRepo.hasSignedContract(tenantId)
    }
}

