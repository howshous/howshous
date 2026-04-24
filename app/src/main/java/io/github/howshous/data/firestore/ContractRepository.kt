package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.Contract
import kotlinx.coroutines.tasks.await

class ContractRepository {
    private val db = FirebaseFirestore.getInstance()
    private val listingRepository = ListingRepository()
    private val activeContractStatuses = setOf("signed", "confirmed", "needs_resign", "active")

    suspend fun createContractFromListingTemplate(
        listingId: String,
        chatId: String,
        landlordId: String,
        tenantId: String,
        fallbackTitle: String,
        fallbackTerms: String,
        fallbackMonthlyRent: Int,
        fallbackDeposit: Int
    ): String {
        return try {
            if (tenantId.isBlank() || landlordId.isBlank() || listingId.isBlank() || chatId.isBlank()) {
                return ""
            }
            val listingDoc = db.collection("listings").document(listingId).get().await()
            val template = listingDoc.get("contractTemplate") as? Map<String, Any>

            val monthlyRent = (template?.get("monthlyRent") as? Number)?.toInt() ?: fallbackMonthlyRent
            val deposit = (template?.get("deposit") as? Number)?.toInt() ?: fallbackDeposit
            val title = template?.get("title") as? String ?: fallbackTitle
            val terms = template?.get("terms") as? String ?: fallbackTerms
            val startDate = template?.get("startDate") as? Timestamp
            val endDate = template?.get("endDate") as? Timestamp

            val contractMap = hashMapOf<String, Any>(
                "chatId" to chatId,
                "listingId" to listingId,
                "landlordId" to landlordId,
                "tenantId" to tenantId,
                "title" to title,
                "terms" to terms,
                "monthlyRent" to monthlyRent,
                "deposit" to deposit,
                "startDate" to (startDate ?: Timestamp.now()),
                "endDate" to (endDate ?: Timestamp.now()),
                "status" to "pending",
                "createdAt" to Timestamp.now()
            )
            val docRef = db.collection("contracts").add(contractMap).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun createContract(contract: Contract): String {
        return try {
            val contractMap = hashMapOf<String, Any>(
                "chatId" to contract.chatId,
                "listingId" to contract.listingId,
                "landlordId" to contract.landlordId,
                "tenantId" to contract.tenantId,
                "title" to contract.title,
                "terms" to contract.terms,
                "monthlyRent" to contract.monthlyRent,
                "deposit" to contract.deposit,
                "startDate" to (contract.startDate ?: Timestamp.now()),
                "endDate" to (contract.endDate ?: Timestamp.now()),
                "status" to contract.status,
                "createdAt" to Timestamp.now()
            )
            val docRef = db.collection("contracts").add(contractMap).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun getContract(contractId: String): Contract? {
        return try {
            val doc = db.collection("contracts").document(contractId).get().await()
            val data = doc.data ?: return null
            Contract(
                id = doc.id,
                chatId = data["chatId"] as? String ?: "",
                listingId = data["listingId"] as? String ?: "",
                landlordId = data["landlordId"] as? String ?: "",
                tenantId = data["tenantId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                terms = data["terms"] as? String ?: "",
                monthlyRent = (data["monthlyRent"] as? Number)?.toInt() ?: 0,
                deposit = (data["deposit"] as? Number)?.toInt() ?: 0,
                startDate = data["startDate"] as? Timestamp,
                endDate = data["endDate"] as? Timestamp,
                status = data["status"] as? String ?: "pending",
                signedAt = data["signedAt"] as? Timestamp,
                createdAt = data["createdAt"] as? Timestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signContract(contractId: String): Boolean {
        return try {
            val contractRef = db.collection("contracts").document(contractId)
            val snap = contractRef.get().await()
            val listingId = snap.getString("listingId") ?: ""
            val tenantId = snap.getString("tenantId") ?: ""
            val landlordId = snap.getString("landlordId") ?: ""
            val tenantProfile = if (tenantId.isNotBlank()) {
                db.collection("users").document(tenantId).get().await()
            } else {
                null
            }
            val tenantName = if (tenantProfile != null && tenantProfile.exists()) {
                val first = tenantProfile.getString("firstName").orEmpty().trim()
                val last = tenantProfile.getString("lastName").orEmpty().trim()
                listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
            } else {
                ""
            }

            contractRef.update(
                mapOf(
                    "status" to "signed",
                    "signedAt" to Timestamp.now()
                )
            ).await()

            if (listingId.isNotBlank() && tenantId.isNotBlank() && landlordId.isNotBlank()) {
                val tenancyId = "${listingId}_$tenantId"
                val tenancyData = mapOf(
                    "listingId" to listingId,
                    "tenantId" to tenantId,
                    "tenantName" to tenantName,
                    "landlordId" to landlordId,
                    "contractId" to contractId,
                    "status" to "signed",
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
                try {
                    db.collection("tenancies").document(tenancyId).set(tenancyData).await()
                } catch (tenancyError: Exception) {
                    tenancyError.printStackTrace()
                }
                try {
                    listingRepository.syncListingOccupancy(listingId)
                } catch (syncError: Exception) {
                    syncError.printStackTrace()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun terminateContract(contractId: String, terminatedBy: String): Boolean {
        return try {
            if (contractId.isBlank()) return false
            db.collection("contracts").document(contractId).update(
                mapOf(
                    "status" to "terminated",
                    "terminatedAt" to Timestamp.now(),
                    "terminatedBy" to terminatedBy
                )
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getContractsForChat(chatId: String, userId: String): List<Contract> {
        return try {
            if (chatId.isBlank() || userId.isBlank()) return emptyList()

            val tenantSnap = db.collection("contracts")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("tenantId", userId)
                .get()
                .await()

            val landlordSnap = db.collection("contracts")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("landlordId", userId)
                .get()
                .await()

            val docs = (tenantSnap.documents + landlordSnap.documents).distinctBy { it.id }
            val contracts = docs.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Contract(
                    id = doc.id,
                    chatId = data["chatId"] as? String ?: "",
                    listingId = data["listingId"] as? String ?: "",
                    landlordId = data["landlordId"] as? String ?: "",
                    tenantId = data["tenantId"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    terms = data["terms"] as? String ?: "",
                    monthlyRent = (data["monthlyRent"] as? Number)?.toInt() ?: 0,
                    deposit = (data["deposit"] as? Number)?.toInt() ?: 0,
                    startDate = data["startDate"] as? Timestamp,
                    endDate = data["endDate"] as? Timestamp,
                    status = data["status"] as? String ?: "pending",
                    signedAt = data["signedAt"] as? Timestamp,
                    createdAt = data["createdAt"] as? Timestamp
                )
            }
            contracts.sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getSignedContractsForTenant(tenantId: String): List<Contract> {
        return try {
            val snap = db.collection("contracts")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            
            val contracts = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Contract(
                    id = doc.id,
                    chatId = data["chatId"] as? String ?: "",
                    listingId = data["listingId"] as? String ?: "",
                    landlordId = data["landlordId"] as? String ?: "",
                    tenantId = data["tenantId"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    terms = data["terms"] as? String ?: "",
                    monthlyRent = (data["monthlyRent"] as? Number)?.toInt() ?: 0,
                    deposit = (data["deposit"] as? Number)?.toInt() ?: 0,
                    startDate = data["startDate"] as? Timestamp,
                    endDate = data["endDate"] as? Timestamp,
                    status = data["status"] as? String ?: "pending",
                    signedAt = data["signedAt"] as? Timestamp,
                    createdAt = data["createdAt"] as? Timestamp
                )
            }.filter { contract ->
                activeContractStatuses.contains(contract.status.lowercase())
            }
            // Sort by signedAt descending (most recent first)
            contracts.sortedByDescending { it.signedAt?.seconds ?: 0L }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun hasSignedContract(tenantId: String): Boolean {
        return try {
            val snap = db.collection("contracts")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            snap.documents.any { doc ->
                val status = doc.getString("status").orEmpty().lowercase()
                activeContractStatuses.contains(status)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSignedContractForTenantAndListing(tenantId: String, listingId: String): Contract? {
        return try {
            val snap = db.collection("contracts")
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            val contracts = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Contract(
                    id = doc.id,
                    chatId = data["chatId"] as? String ?: "",
                    listingId = data["listingId"] as? String ?: "",
                    landlordId = data["landlordId"] as? String ?: "",
                    tenantId = data["tenantId"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    terms = data["terms"] as? String ?: "",
                    monthlyRent = (data["monthlyRent"] as? Number)?.toInt() ?: 0,
                    deposit = (data["deposit"] as? Number)?.toInt() ?: 0,
                    startDate = data["startDate"] as? Timestamp,
                    endDate = data["endDate"] as? Timestamp,
                    status = data["status"] as? String ?: "pending",
                    signedAt = data["signedAt"] as? Timestamp,
                    createdAt = data["createdAt"] as? Timestamp
                )
            }.filter { activeContractStatuses.contains(it.status.lowercase()) }

            contracts.maxByOrNull { it.signedAt?.seconds ?: 0L }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

