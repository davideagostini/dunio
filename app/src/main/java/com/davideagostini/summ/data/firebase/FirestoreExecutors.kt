package com.davideagostini.summ.data.firebase

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shared executors for Firestore callbacks.
 *
 * Snapshot listeners dispatch on the main thread by default. For larger household datasets that
 * can push document deserialization and mapping onto the UI thread, so realtime DAO listeners use
 * this background executor instead.
 */
object FirestoreExecutors {
    private val listenerExecutorService: ExecutorService = Executors.newFixedThreadPool(2)

    val listenerExecutor: Executor = listenerExecutorService
}
