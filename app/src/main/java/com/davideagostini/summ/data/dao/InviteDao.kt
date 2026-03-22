package com.davideagostini.summ.data.dao

import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class InviteDao(
    private val db: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {
    fun getAllInvites(): Flow<List<Invite>> {
        val firestore = db ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { state ->
            when (state) {
                is SessionState.Ready -> {
                    firestoreFlow<List<Invite>> { emit ->
                        firestore.collection(FirestorePaths.invites(state.household.id))
                            .addSnapshotListener { snapshot, error ->
                                when {
                                    error != null -> emit(Result.failure(error))
                                    snapshot != null -> emit(
                                        Result.success(
                                            snapshot.documents
                                                .mapNotNull { document ->
                                                    document.toObject(InviteDocument::class.java)?.toInvite(document.id)
                                                }
                                                .sortedWith(compareBy<Invite> { it.status }.thenBy { it.email.lowercase() })
                                        )
                                    )
                                }
                            }
                    }.map { result -> result.getOrElse { emptyList() } }
                }
                else -> flowOf(emptyList())
            }
        }
    }

    suspend fun createInvite(email: String, role: String) {
        val firestore = db ?: return
        val householdId = sessionRepository.requireHouseholdId()
        firestore.collection(FirestorePaths.invites(householdId))
            .add(
                mapOf(
                    "email" to email.trim().lowercase(),
                    "role" to role,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            .await()
    }
}

private data class InviteDocument(
    val email: String = "",
    val role: String = "member",
    val status: String = "pending",
) {
    fun toInvite(id: String): Invite = Invite(
        id = id,
        email = email,
        role = role,
        status = status,
    )
}
