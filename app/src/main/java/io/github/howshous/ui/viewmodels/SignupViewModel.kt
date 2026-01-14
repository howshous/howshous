package io.github.howshous.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import io.github.howshous.ui.util.uploadCompressedImage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue


class SignupViewModel : ViewModel() {

    // BASIC INFO
    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName

    private val _contact = MutableStateFlow("")   // email only for now
    val contact: StateFlow<String> = _contact

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _role = MutableStateFlow("")  // "tenant" or "landlord"
    val role: StateFlow<String> = _role


    // TENANT IMAGES
    fun setTenantImage(step: Int, uri: Uri) {
        when (step) {
            1 -> _tenantSelfie.value = uri
            2 -> _tenantId.value = uri
        }
    }
    private val _tenantSelfie = MutableStateFlow<Uri?>(null)
    val tenantSelfie: StateFlow<Uri?> = _tenantSelfie

    private val _tenantId = MutableStateFlow<Uri?>(null)
    val tenantId: StateFlow<Uri?> = _tenantId



    // LANDLORD IMAGES
    fun setLandlordImage(step: Int, uri: Uri) {
        when (step) {
            1 -> _landlordSelfie.value = uri
            2 -> _landlordId.value = uri
            3 -> _landlordPropertyDoc.value = uri
        }
    }
    private val _landlordSelfie = MutableStateFlow<Uri?>(null)
    val landlordSelfie: StateFlow<Uri?> = _landlordSelfie

    private val _landlordId = MutableStateFlow<Uri?>(null)
    val landlordId: StateFlow<Uri?> = _landlordId

    private val _landlordPropertyDoc = MutableStateFlow<Uri?>(null)
    val landlordPropertyDoc: StateFlow<Uri?> = _landlordPropertyDoc


    // ─────────────────────────────────────────────
    // SETTERS
    // ─────────────────────────────────────────────

    fun setFirstName(v: String) { _firstName.value = v }
    fun setLastName(v: String) { _lastName.value = v }
    fun setContact(v: String) { _contact.value = v }
    fun setPassword(v: String) { _password.value = v }
    fun setRole(v: String) { _role.value = v }

    // TENANT
    suspend fun finishTenantSignup(
        context: Context
    ): Result<Unit> {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val email = contact.value
        val password = password.value

        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password are required."))
        }

        val uid = try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val displayName = "${firstName.value} ${lastName.value}".trim()
            if (displayName.isNotEmpty()) {
                val request = userProfileChangeRequest { this.displayName = displayName }
                result.user?.updateProfile(request)?.await()
            }
            result.user!!.uid
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val selfieUri = tenantSelfie.value
            ?: return Result.failure(IllegalStateException("Selfie is missing."))
        val profileUrl = uploadCompressedImage(
            context,
            selfieUri,
            "users/$uid/profile.jpg"
        )

        val idUri = tenantId.value
            ?: return Result.failure(IllegalStateException("ID photo is missing."))

        val selfieVerificationUrl = uploadCompressedImage(
            context,
            selfieUri,
            "verifications/$uid/selfie.jpg"
        )

        val idVerificationUrl = uploadCompressedImage(
            context,
            idUri,
            "verifications/$uid/id.jpg"
        )

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to firstName.value,
            "lastName" to lastName.value,
            "email" to email,
            "role" to "tenant",
            "verified" to false,
            "profileImageUrl" to profileUrl
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

        FirebaseAuth.getInstance().signOut()

        return Result.success(Unit)
    }

    // LANDLORD
    suspend fun finishLandlordSignup(
        context: Context
    ): Result<Unit> {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val email = contact.value
        val password = password.value

        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password are required."))
        }

        val uid = try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val displayName = "${firstName.value} ${lastName.value}".trim()
            if (displayName.isNotEmpty()) {
                val request = userProfileChangeRequest { this.displayName = displayName }
                result.user?.updateProfile(request)?.await()
            }
            result.user!!.uid
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // Upload profile (selfie)
        val selfieUri = landlordSelfie.value
            ?: return Result.failure(IllegalStateException("Selfie is missing."))
        val profileUrl = uploadCompressedImage(
            context,
            selfieUri,
            "users/$uid/profile.jpg"
        )

        // Upload verification files
        val idUri = landlordId.value
            ?: return Result.failure(IllegalStateException("ID photo is missing."))
        val propertyUri = landlordPropertyDoc.value
            ?: return Result.failure(IllegalStateException("Property document is missing."))

        val selfieVerificationUrl = uploadCompressedImage(context, selfieUri, "verifications/$uid/selfie.jpg")
        val idVerificationUrl = uploadCompressedImage(context, idUri, "verifications/$uid/id.jpg")
        val propertyVerificationUrl = uploadCompressedImage(context, propertyUri, "verifications/$uid/property.jpg")

        // User doc
        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to firstName.value,
            "lastName" to lastName.value,
            "email" to email,
            "role" to "landlord",
            "verified" to false,
            "profileImageUrl" to profileUrl
        )

        db.collection("users").document(uid).set(userDoc).await()

        // Verification doc
        val verificationDoc = hashMapOf(
            "selfieUrl" to selfieVerificationUrl,
            "idUrl" to idVerificationUrl,
            "propertyUrl" to propertyVerificationUrl,
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        db.collection("verifications").document(uid).set(verificationDoc).await()

        FirebaseAuth.getInstance().signOut()

        return Result.success(Unit)
    }



    fun clearAll() {
        _firstName.value = ""
        _lastName.value = ""
        _contact.value = ""
        _password.value = ""
        _role.value = ""

        _tenantSelfie.value = null
        _tenantId.value = null

        _landlordSelfie.value = null
        _landlordId.value = null
        _landlordPropertyDoc.value = null
    }
}
