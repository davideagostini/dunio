package com.davideagostini.summ.data.dao

import com.google.firebase.firestore.FirebaseFirestore
import com.davideagostini.summ.data.entity.Member
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class MemberDao(
    private val db: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {
    fun getAllMembers(): Flow<List<Member>> {
        val firestore = db ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { state ->
            when (state) {
                is SessionState.Ready -> {
                    firestoreFlow<List<Member>> { emit ->
                        firestore.collection(FirestorePaths.members(state.household.id))
                            .addSnapshotListener { snapshot, error ->
                                when {
                                    error != null -> emit(Result.failure(error))
                                    snapshot != null -> emit(
                                        Result.success(
                                            snapshot.documents
                                                .mapNotNull { document ->
                                                    document.toObject(MemberDocument::class.java)?.toMember(document.id)
                                                }
                                                .sortedWith(
                                                    compareBy<Member> { it.name.ifBlank { it.email.ifBlank { it.userId } }.lowercase() }
                                                        .thenBy { it.userId.lowercase() }
                                                )
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
}

private data class MemberDocument(
    val userId: String = "",
    val role: String = "member",
    val name: String = "",
    val email: String = "",
    val photoURL: String? = null,
) {
    fun toMember(documentId: String): Member = Member(
        userId = userId.ifBlank { documentId },
        role = role,
        name = name,
        email = email,
        photoUrl = photoURL,
    )
}
