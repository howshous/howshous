package io.github.howshous.ui.util

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.R
import io.github.howshous.data.firestore.AnalyticsRepository
import io.github.howshous.data.firestore.ListingRepository
import kotlinx.coroutines.tasks.await

object SampleAccountSeeder {

    data class SeedResult(
        val created: Boolean,
        val message: String
    )

    private const val TENANT_FIRST = "Timmy"
    private const val TENANT_LAST = "Tenant"
    private const val TENANT_EMAIL = "tt@hh.com"
    private const val TENANT_PASSWORD = "timmytimmy"

    private const val LANDLORD_FIRST = "Lemm"
    private const val LANDLORD_LAST = "Landlord"
    private const val LANDLORD_EMAIL = "ll@hh.com"
    private const val LANDLORD_PASSWORD = "lemmlemm"

    private const val ADMIN_FIRST = "Annie"
    private const val ADMIN_LAST = "Admin"
    private const val ADMIN_EMAIL = "aa@hh.com"
    private const val ADMIN_PASSWORD = "annieannie"

    suspend fun generateIfMissing(context: Context): SeedResult {
        val auth = FirebaseAuth.getInstance()

        return try {
            val tenantUid = ensureAccount(
                auth = auth,
                email = TENANT_EMAIL,
                password = TENANT_PASSWORD
            ) { createTenantAccount(context) }
            auth.signOut()

            val landlordUid = ensureAccount(
                auth = auth,
                email = LANDLORD_EMAIL,
                password = LANDLORD_PASSWORD
            ) { createLandlordAccount(context) }
            auth.signOut()

            // Listing writes are expected to be authored by the landlord account.
            auth.signInWithEmailAndPassword(LANDLORD_EMAIL, LANDLORD_PASSWORD).await()

            val createdIds = SampleListingsGenerator.generateSampleListings(landlordUid)
            auth.signOut()

            val adminUid = ensureAccount(
                auth = auth,
                email = ADMIN_EMAIL,
                password = ADMIN_PASSWORD
            ) { createAdminAccount(context) }
            val listingRepo = ListingRepository()
            val listings = listingRepo.getListingsForLandlord(landlordUid)
            if (listings.isNotEmpty()) {
                val analyticsRepo = AnalyticsRepository()
                analyticsRepo.seedTestEventsForLandlord(
                    landlordId = landlordUid,
                    listings = listings.map { it.id to it.price }
                )
            }

            auth.signOut()

            SeedResult(
                true,
                "Sample accounts ready (tenant=$tenantUid, landlord=$landlordUid, admin=$adminUid). Recreated listings: ${createdIds.size}."
            )
        } catch (e: Exception) {
            auth.signOut()
            SeedResult(false, "Failed to generate sample data: ${e.message}")
        }
    }

    private suspend fun hasAccountForEmail(auth: FirebaseAuth, email: String): Boolean {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
            !methods.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun ensureAccount(
        auth: FirebaseAuth,
        email: String,
        password: String,
        createNew: suspend () -> String
    ): String {
        return if (hasAccountForEmail(auth, email)) {
            resolveExistingUid(auth, email, password)
        } else {
            createNew()
        }
    }

    private suspend fun resolveExistingUid(
        auth: FirebaseAuth,
        email: String,
        password: String
    ): String {
        val byDoc = findUserUidByEmail(email)
        if (!byDoc.isNullOrBlank()) return byDoc

        val signIn = auth.signInWithEmailAndPassword(email, password).await()
        return signIn.user?.uid
            ?: throw IllegalStateException("Unable to resolve UID for existing account: $email")
    }

    private suspend fun findUserUidByEmail(email: String): String? {
        val db = FirebaseFirestore.getInstance()
        val snap = db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.id
    }

    private suspend fun createTenantAccount(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(TENANT_EMAIL, TENANT_PASSWORD).await()
        val uid = res.user!!.uid
        val displayName = "$TENANT_FIRST $TENANT_LAST"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val idUri = resUri(context, R.drawable.test_id_card)

        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")
        val selfieVerificationUrl = uploadCompressedImage(context, selfieUri, "verifications/$uid/selfie.jpg")
        val idVerificationUrl = uploadCompressedImage(context, idUri, "verifications/$uid/id.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to TENANT_FIRST,
            "lastName" to TENANT_LAST,
            "email" to TENANT_EMAIL,
            "role" to "tenant",
            "verified" to false,
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to ""
        )

        db.collection("users").document(uid).set(userDoc).await()

        val verificationDoc = hashMapOf(
            "selfieUrl" to selfieVerificationUrl,
            "idUrl" to idVerificationUrl,
            "propertyUrl" to "",
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        db.collection("verifications").document(uid).set(verificationDoc).await()

        return uid
    }

    private suspend fun createLandlordAccount(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(LANDLORD_EMAIL, LANDLORD_PASSWORD).await()
        val uid = res.user!!.uid
        val displayName = "$LANDLORD_FIRST $LANDLORD_LAST"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val idUri = resUri(context, R.drawable.test_id_card)
        val permitUri = resUri(context, R.drawable.test_permit)

        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")
        val selfieVerificationUrl = uploadCompressedImage(context, selfieUri, "verifications/$uid/selfie.jpg")
        val idVerificationUrl = uploadCompressedImage(context, idUri, "verifications/$uid/id.jpg")
        val propertyVerificationUrl = uploadCompressedImage(context, permitUri, "verifications/$uid/property.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to LANDLORD_FIRST,
            "lastName" to LANDLORD_LAST,
            "email" to LANDLORD_EMAIL,
            "role" to "landlord",
            "verified" to false,
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to propertyVerificationUrl
        )

        db.collection("users").document(uid).set(userDoc).await()

        val verificationDoc = hashMapOf(
            "selfieUrl" to selfieVerificationUrl,
            "idUrl" to idVerificationUrl,
            "propertyUrl" to propertyVerificationUrl,
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        db.collection("verifications").document(uid).set(verificationDoc).await()

        return uid
    }

    private suspend fun createAdminAccount(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
        val uid = res.user!!.uid
        val displayName = "$ADMIN_FIRST $ADMIN_LAST"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to ADMIN_FIRST,
            "lastName" to ADMIN_LAST,
            "email" to ADMIN_EMAIL,
            "role" to "administrator",
            "verified" to true,
            "isBanned" to false,
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to ""
        )

        db.collection("users").document(uid).set(userDoc).await()

        return uid
    }

    private fun resUri(context: Context, @DrawableRes resId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }
}
