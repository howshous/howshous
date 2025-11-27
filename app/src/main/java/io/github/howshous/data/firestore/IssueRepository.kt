package io.github.howshous.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import io.github.howshous.data.models.Issue
import kotlinx.coroutines.tasks.await

class IssueRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createIssue(issue: Issue): String {
        return try {
            val docRef = db.collection("issues").add(issue).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun getIssuesForLandlord(landlordId: String, limit: Int = 10): List<Issue> {
        return try {
            val snap = db.collection("issues")
                .whereEqualTo("landlordId", landlordId)
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Issue::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getIssue(issueId: String): Issue? {
        return try {
            val doc = db.collection("issues").document(issueId).get().await()
            doc.toObject(Issue::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resolveIssue(issueId: String, resolutionPhotoUrl: String) {
        try {
            db.collection("issues").document(issueId).update(
                mapOf(
                    "status" to "resolved",
                    "resolutionPhotoUrl" to resolutionPhotoUrl,
                    "resolvedAt" to Timestamp.now()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getPendingIssuesForLandlord(landlordId: String): List<Issue> {
        return try {
            val snap = db.collection("issues")
                .whereEqualTo("landlordId", landlordId)
                .whereEqualTo("status", "pending")
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Issue::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

