package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.Contract
import kotlinx.coroutines.tasks.await

class ContractRepository {
    private val db = FirebaseFirestore.getInstance()

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
            db.collection("contracts").document(contractId).update(
                mapOf(
                    "status" to "signed",
                    "signedAt" to Timestamp.now()
                )
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getContractsForChat(chatId: String): List<Contract> {
        return try {
            val snap = db.collection("contracts")
                .whereEqualTo("chatId", chatId)
                .orderBy("createdAt")
                .get()
                .await()
            
            snap.documents.mapNotNull { doc ->
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
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getSignedContractsForTenant(tenantId: String): List<Contract> {
        return try {
            val snap = db.collection("contracts")
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", "signed")
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
                .whereEqualTo("status", "signed")
                .limit(1)
                .get()
                .await()
            !snap.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

