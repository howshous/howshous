package io.github.howshous.ui.util

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.google.firebase.auth.FirebaseAuth
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

    private const val TENANT2_FIRST = "Terry"
    private const val TENANT2_LAST = "Benevolence"
    private const val TENANT2_EMAIL = "tb@hh.com"
    private const val TENANT2_PASSWORD = "terryterry"

    private const val LANDLORD_FIRST = "Lemm"
    private const val LANDLORD_LAST = "Landlord"
    private const val LANDLORD_EMAIL = "ll@hh.com"
    private const val LANDLORD_PASSWORD = "lemmlemm"

    private const val LANDLORD2_FIRST = "Larry"
    private const val LANDLORD2_LAST = "Bantrot"
    private const val LANDLORD2_EMAIL = "lb@hh.com"
    private const val LANDLORD2_PASSWORD = "larrylarry"

    private const val ADMIN_FIRST = "Annie"
    private const val ADMIN_LAST = "Admin"
    private const val ADMIN_EMAIL = "aa@hh.com"
    private const val ADMIN_PASSWORD = "annieannie"

    suspend fun generateIfMissing(context: Context): SeedResult {
        val auth = FirebaseAuth.getInstance()

        return try {
            val tenantUid = ensureAccount(
                auth,
                TENANT_EMAIL,
                TENANT_PASSWORD
            ) { createTenantAccount(context, TENANT_FIRST, TENANT_LAST, TENANT_EMAIL, TENANT_PASSWORD) }
            auth.signOut()

            val tenant2Uid = ensureAccount(
                auth,
                TENANT2_EMAIL,
                TENANT2_PASSWORD
            ) { createTenantAccount(context, TENANT2_FIRST, TENANT2_LAST, TENANT2_EMAIL, TENANT2_PASSWORD) }
            auth.signOut()

            val landlordUid = ensureAccount(
                auth,
                LANDLORD_EMAIL,
                LANDLORD_PASSWORD
            ) { createLandlordAccount(context, LANDLORD_FIRST, LANDLORD_LAST, LANDLORD_EMAIL, LANDLORD_PASSWORD) }
            auth.signOut()

            val landlord2Uid = ensureAccount(
                auth,
                LANDLORD2_EMAIL,
                LANDLORD2_PASSWORD
            ) { createLandlordAccount(context, LANDLORD2_FIRST, LANDLORD2_LAST, LANDLORD2_EMAIL, LANDLORD2_PASSWORD) }
            auth.signOut()

            auth.signInWithEmailAndPassword(LANDLORD_EMAIL, LANDLORD_PASSWORD).await()
            SampleListingsGenerator.generateSampleListings(landlordUid, context)
            auth.signOut()

            auth.signInWithEmailAndPassword(LANDLORD2_EMAIL, LANDLORD2_PASSWORD).await()
            SampleListingsGenerator.generateSampleListings(
                landlord2Uid,
                context,
                "Larry's"
            )
            auth.signOut()

            ensureAccount(
                auth,
                ADMIN_EMAIL,
                ADMIN_PASSWORD
            ) { createAdminAccount(context) }

            val listingRepo = ListingRepository()
            val analyticsRepo = AnalyticsRepository()

            val lemmListings = listingRepo.getListingsForLandlord(landlordUid)
            if (lemmListings.isNotEmpty()) {
                analyticsRepo.seedTestEventsForLandlord(
                    landlordUid,
                    lemmListings.map { it.id to it.price }
                )
            }

            val larryListings = listingRepo.getListingsForLandlord(landlord2Uid)
            if (larryListings.isNotEmpty()) {
                analyticsRepo.seedTestEventsForLandlord(
                    landlord2Uid,
                    larryListings.map { it.id to it.price }
                )
            }

            auth.signOut()

            SeedResult(true, "done")
        } catch (e: Exception) {
            auth.signOut()
            SeedResult(false, e.message ?: "error")
        }
    }

    private fun getProfileResId(context: Context, firstName: String): Int {
        val resName = "test_pfp_${firstName.lowercase()}"
        val resId = context.resources.getIdentifier(
            resName,
            "drawable",
            context.packageName
        )
        return if (resId != 0) resId else R.drawable.test_pfp
    }

    private suspend fun ensureAccount(
        auth: FirebaseAuth,
        email: String,
        password: String,
        createNew: suspend () -> String
    ): String {
        return if (hasAccountForEmail(auth, email)) {
            resolveExistingUid(auth, email, password)
        } else createNew()
    }

    private suspend fun hasAccountForEmail(auth: FirebaseAuth, email: String): Boolean {
        return try {
            val res = auth.fetchSignInMethodsForEmail(email).await()
            !res.signInMethods.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun resolveExistingUid(
        auth: FirebaseAuth,
        email: String,
        password: String
    ): String {
        val signIn = auth.signInWithEmailAndPassword(email, password).await()
        return signIn.user!!.uid
    }

    private suspend fun createTenantAccount(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user!!.uid

        val selfieUri = resUri(context, getProfileResId(context, firstName))
        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")

        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "role" to "tenant",
                "profileImageUrl" to profileUrl
            )
        ).await()

        return uid
    }

    private suspend fun createLandlordAccount(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user!!.uid

        val selfieUri = resUri(context, getProfileResId(context, firstName))
        val permitUri = resUri(context, R.drawable.test_permit)

        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")
        val permitUrl = uploadCompressedImage(context, permitUri, "verifications/$uid/property.jpg")

        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "role" to "landlord",
                "profileImageUrl" to profileUrl,
                "businessPermitUrl" to permitUrl
            )
        ).await()

        return uid
    }

    private suspend fun createAdminAccount(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
        val uid = res.user!!.uid

        val selfieUri = resUri(context, getProfileResId(context, ADMIN_FIRST))
        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")

        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "firstName" to ADMIN_FIRST,
                "lastName" to ADMIN_LAST,
                "email" to ADMIN_EMAIL,
                "role" to "administrator",
                "profileImageUrl" to profileUrl
            )
        ).await()

        return uid
    }

    private fun resUri(context: Context, @DrawableRes resId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }
}
