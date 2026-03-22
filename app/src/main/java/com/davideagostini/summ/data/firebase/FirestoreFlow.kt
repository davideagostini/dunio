package com.davideagostini.summ.data.firebase

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> firestoreFlow(register: (emit: (Result<T>) -> Unit) -> ListenerRegistration): Flow<Result<T>> =
    callbackFlow {
        val listener = register { result -> trySend(result) }
        awaitClose { listener.remove() }
    }
