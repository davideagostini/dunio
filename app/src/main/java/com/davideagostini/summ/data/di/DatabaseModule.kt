package com.davideagostini.summ.data.di

import android.content.Context
import com.davideagostini.summ.data.dao.AssetDao
import com.davideagostini.summ.data.dao.CategoryDao
import com.davideagostini.summ.data.dao.EntryDao
import com.davideagostini.summ.data.dao.InviteDao
import com.davideagostini.summ.data.dao.MemberDao
import com.davideagostini.summ.data.dao.MonthCloseDao
import com.davideagostini.summ.data.dao.RecurringTransactionDao
import com.davideagostini.summ.data.session.SessionRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAssetDao(
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): AssetDao = AssetDao(firestore, sessionRepository)

    @Provides
    fun provideEntryDao(
        @ApplicationContext appContext: Context,
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): EntryDao = EntryDao(appContext, firestore, sessionRepository)

    @Provides
    fun provideCategoryDao(
        @ApplicationContext appContext: Context,
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): CategoryDao = CategoryDao(appContext, firestore, sessionRepository)

    @Provides
    fun provideMemberDao(
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): MemberDao = MemberDao(firestore, sessionRepository)

    @Provides
    fun provideInviteDao(
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): InviteDao = InviteDao(firestore, sessionRepository)

    @Provides
    fun provideRecurringTransactionDao(
        @ApplicationContext appContext: Context,
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): RecurringTransactionDao = RecurringTransactionDao(appContext, firestore, sessionRepository)

    @Provides
    fun provideMonthCloseDao(
        firestore: FirebaseFirestore?,
        sessionRepository: SessionRepository,
    ): MonthCloseDao = MonthCloseDao(firestore, sessionRepository)
}
