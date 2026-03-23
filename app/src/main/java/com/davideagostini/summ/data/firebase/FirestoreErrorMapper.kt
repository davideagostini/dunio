package com.davideagostini.summ.data.firebase

import android.content.Context
import com.davideagostini.summ.R
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Maps low-level Firestore failures to short user-facing messages that can be shown directly in the UI.
 *
 * The app performs most writes from ViewModels. When a write is rejected by security rules,
 * Firestore throws on the coroutine and, without interception, that failure bubbles up as a crash.
 * This mapper keeps the handling centralized so every screen can show the same wording.
 */
fun Throwable.toFirestoreUserMessage(context: Context): String {
    val firestoreError = findFirestoreException()

    return when (firestoreError?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            context.getString(R.string.firestore_permission_denied)

        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            context.getString(R.string.firestore_unauthenticated)

        FirebaseFirestoreException.Code.UNAVAILABLE ->
            context.getString(R.string.firestore_unavailable)

        else ->
            context.getString(R.string.firestore_generic_write_error)
    }
}

/**
 * Firestore errors can be wrapped by coroutine or repository layers, so walk the cause chain until we find
 * the real backend exception that contains the canonical Firestore error code.
 */
private fun Throwable.findFirestoreException(): FirebaseFirestoreException? =
    generateSequence(this) { current -> current.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
