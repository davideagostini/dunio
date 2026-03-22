package com.davideagostini.summ.data.di

import android.content.Context
import com.davideagostini.summ.data.firebase.FirebaseConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth? =
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseAuth.getInstance()
        } else {
            null
        }

    @Provides
    @Singleton
    fun provideFirestore(@ApplicationContext context: Context): FirebaseFirestore? =
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseFirestore.getInstance()
        } else {
            null
        }

    @Provides
    @Singleton
    fun provideDefaultWebClientId(@ApplicationContext context: Context): String? =
        FirebaseConfig.getDefaultWebClientId(context)
}
