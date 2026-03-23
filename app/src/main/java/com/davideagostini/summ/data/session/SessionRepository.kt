package com.davideagostini.summ.data.session

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.AppUser
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Household
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.widget.SummWidgetsUpdater
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

private data class UserDocument(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoURL: String? = null,
    val householdId: String? = null,
    val createdAt: Timestamp? = null,
)

private data class HouseholdDocument(
    val name: String = "",
    val ownerId: String = "",
    val createdAt: Timestamp? = null,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepository @Inject constructor(
    @param:ApplicationContext
    private val appContext: Context,
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val defaultWebClientId: String?,
) {
    private val defaultCategories = listOf(
        Category(name = appContext.getString(R.string.default_category_food), emoji = "🍕"),
        Category(name = appContext.getString(R.string.default_category_transport), emoji = "🚗"),
        Category(name = appContext.getString(R.string.default_category_home), emoji = "🏠"),
        Category(name = appContext.getString(R.string.default_category_health), emoji = "💊"),
        Category(name = appContext.getString(R.string.default_category_leisure), emoji = "🎉"),
        Category(name = appContext.getString(R.string.default_category_work), emoji = "💼"),
        Category(name = appContext.getString(R.string.default_category_other), emoji = "📦"),
    )

    val sessionState: Flow<SessionState> =
        if (auth == null || firestore == null) {
            flowOf(SessionState.ConfigurationError(appContext.getString(R.string.session_configuration_error)))
        } else {
            authStateFlow(auth).flatMapLatest { firebaseUser ->
                if (firebaseUser == null) {
                    flowOf(SessionState.SignedOut)
                } else {
                    userSessionFlow(firebaseUser)
                }
            }
        }

    suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)
        val idToken = getGoogleIdToken(
            credentialManager = credentialManager,
            context = context,
            filterByAuthorizedAccounts = false,
        )
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val firebaseAuth = requireNotNull(auth) { appContext.getString(R.string.session_auth_unavailable) }

        firebaseAuth.signInWithCredential(credential).await()
        upsertUser(firebaseAuth.currentUser ?: error(appContext.getString(R.string.session_sign_in_missing_user)))
        // Widgets depend on both auth state and household membership. Refresh right after
        // sign-in so launcher content does not lag behind the app session.
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun signOut() {
        auth?.signOut()
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }
        // Clears household-aware widgets back to their signed-out fallback state.
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun createHousehold(name: String) {
        val firebaseUser = requireNotNull(auth?.currentUser) { appContext.getString(R.string.session_sign_in_required) }
        val db = requireNotNull(firestore) { appContext.getString(R.string.session_firestore_unavailable) }
        require(name.trim().isNotEmpty()) { appContext.getString(R.string.session_household_name_required) }
        val householdRef = db.collection("households").document()
        val batch = db.batch()

        batch.set(
            householdRef,
            mapOf(
                "name" to name.trim(),
                "ownerId" to firebaseUser.uid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        )

        batch.set(
            db.document(FirestorePaths.member(householdRef.id, firebaseUser.uid)),
            mapOf(
                "userId" to firebaseUser.uid,
                "role" to "owner",
                "joinedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        )

        defaultCategories.forEach { category ->
            val categoryId = encodeCategoryId(category.name)
            batch.set(
                db.document(FirestorePaths.category(householdRef.id, categoryId)),
                mapOf(
                    "name" to category.name,
                    "type" to category.type,
                    "icon" to category.emoji,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
        }

        batch.set(
            db.document(FirestorePaths.user(firebaseUser.uid)),
            mapOf(
                "uid" to firebaseUser.uid,
                "email" to (firebaseUser.email ?: ""),
                "name" to (firebaseUser.displayName ?: firebaseUser.email ?: appContext.getString(R.string.session_default_member_name)),
                "photoURL" to firebaseUser.photoUrl?.toString(),
                "householdId" to householdRef.id,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
            SetOptions.merge()
        )

        batch.commit().await()
        // A newly created household changes widget eligibility immediately.
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun joinHousehold(householdId: String) {
        val firebaseUser = requireNotNull(auth?.currentUser) { appContext.getString(R.string.session_sign_in_required) }
        val db = requireNotNull(firestore) { appContext.getString(R.string.session_firestore_unavailable) }
        val trimmedId = householdId.trim()
        require(trimmedId.isNotEmpty()) { appContext.getString(R.string.session_household_id_required) }
        val householdSnapshot = db.document(FirestorePaths.household(trimmedId)).get().await()

        if (!householdSnapshot.exists()) {
            error(appContext.getString(R.string.session_household_not_found))
        }

        val batch = db.batch()
        batch.set(
            db.document(FirestorePaths.member(trimmedId, firebaseUser.uid)),
            mapOf(
                "userId" to firebaseUser.uid,
                "role" to "member",
                "joinedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        )
        batch.set(
            db.document(FirestorePaths.user(firebaseUser.uid)),
            mapOf(
                "uid" to firebaseUser.uid,
                "email" to (firebaseUser.email ?: ""),
                "name" to (firebaseUser.displayName ?: firebaseUser.email ?: appContext.getString(R.string.session_default_member_name)),
                "photoURL" to firebaseUser.photoUrl?.toString(),
                "householdId" to trimmedId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
            SetOptions.merge()
        )
        batch.commit().await()
        // Joining an existing household unlocks widget data for the first time on a new device.
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun requireHouseholdId(): String {
        val currentUser = requireNotNull(auth?.currentUser) { appContext.getString(R.string.session_sign_in_required) }
        val snapshot = requireNotNull(firestore)
            .document(FirestorePaths.user(currentUser.uid))
            .get()
            .await()

        return snapshot.getString("householdId")
            ?.takeIf(String::isNotBlank)
            ?: error(appContext.getString(R.string.session_household_required))
    }

    fun requireFirebaseUser(): FirebaseUser =
        requireNotNull(auth?.currentUser) { appContext.getString(R.string.session_sign_in_required) }

    private suspend fun upsertUser(firebaseUser: FirebaseUser) {
        requireNotNull(firestore)
            .document(FirestorePaths.user(firebaseUser.uid))
            .set(
                mapOf(
                    "uid" to firebaseUser.uid,
                    "email" to (firebaseUser.email ?: ""),
                    "name" to (firebaseUser.displayName ?: firebaseUser.email ?: appContext.getString(R.string.session_default_member_name)),
                    "photoURL" to firebaseUser.photoUrl?.toString(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun userSessionFlow(firebaseUser: FirebaseUser): Flow<SessionState> {
        val db = requireNotNull(firestore)
        return callbackFlow {
            var householdRegistration: com.google.firebase.firestore.ListenerRegistration? = null

            val registration = db.document(FirestorePaths.user(firebaseUser.uid))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(SessionState.ConfigurationError(error.message ?: appContext.getString(R.string.session_load_error)))
                        return@addSnapshotListener
                    }

                    householdRegistration?.remove()
                    householdRegistration = null

                    if (snapshot == null || !snapshot.exists()) {
                        trySend(SessionState.Loading)
                        return@addSnapshotListener
                    }

                    val document = snapshot.toObject(UserDocument::class.java) ?: UserDocument(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        name = firebaseUser.displayName ?: firebaseUser.email ?: appContext.getString(R.string.session_default_member_name),
                        photoURL = firebaseUser.photoUrl?.toString(),
                    )

                    val user = AppUser(
                        uid = firebaseUser.uid,
                        email = document.email.ifBlank { firebaseUser.email.orEmpty() },
                        name = document.name.ifBlank { firebaseUser.displayName ?: firebaseUser.email ?: appContext.getString(R.string.session_default_member_name) },
                        photoUrl = document.photoURL ?: firebaseUser.photoUrl?.toString(),
                        householdId = document.householdId,
                    )

                    val householdId = document.householdId
                    if (householdId.isNullOrBlank()) {
                        // During sign-in we can briefly see a cached/stale user document before
                        // Firestore returns the server copy that already contains the household.
                        if (snapshot.metadata.isFromCache) {
                            trySend(SessionState.Loading)
                        } else {
                            trySend(SessionState.NeedsHousehold(user))
                        }
                        return@addSnapshotListener
                    }

                    trySend(SessionState.Loading)

                    householdRegistration = db.document(FirestorePaths.household(householdId))
                        .addSnapshotListener { householdSnapshot, householdError ->
                            if (householdError != null) {
                                trySend(SessionState.ConfigurationError(householdError.message ?: appContext.getString(R.string.session_household_load_error)))
                                return@addSnapshotListener
                            }

                            val household = householdSnapshot?.toObject(HouseholdDocument::class.java)
                            if (householdSnapshot == null || !householdSnapshot.exists() || household == null) {
                                if (householdSnapshot?.metadata?.isFromCache == true) {
                                    trySend(SessionState.Loading)
                                } else {
                                    trySend(SessionState.NeedsHousehold(user.copy(householdId = null)))
                                }
                            } else {
                                trySend(
                                    SessionState.Ready(
                                        user = user,
                                        household = Household(
                                            id = householdSnapshot.id,
                                            name = household.name,
                                            ownerId = household.ownerId,
                                        )
                                    )
                                )
                            }
                        }
                }

            awaitClose {
                householdRegistration?.remove()
                registration.remove()
            }
        }
    }

    private fun authStateFlow(firebaseAuth: FirebaseAuth): Flow<FirebaseUser?> =
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            firebaseAuth.addAuthStateListener(listener)
            trySend(firebaseAuth.currentUser)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }

    private fun encodeCategoryId(name: String): String =
        URLEncoder.encode(name.trim().lowercase(), StandardCharsets.UTF_8.toString())

    private suspend fun getGoogleIdToken(
        credentialManager: CredentialManager,
        context: Context,
        filterByAuthorizedAccounts: Boolean,
    ): String {
        val webClientId = requireNotNull(defaultWebClientId) {
            appContext.getString(R.string.session_google_not_configured)
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(
                context = context,
                request = request,
            )
        } catch (exception: GetCredentialCancellationException) {
            error(appContext.getString(R.string.session_google_canceled))
        } catch (exception: GetCredentialException) {
            throw exception
        }

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return requireNotNull(googleIdTokenCredential.idToken) {
                appContext.getString(R.string.session_google_missing_token)
            }
        }

        error(appContext.getString(R.string.session_google_unsupported_credential))
    }
}
