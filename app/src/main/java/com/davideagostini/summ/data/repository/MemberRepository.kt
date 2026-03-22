package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.InviteDao
import com.davideagostini.summ.data.dao.MemberDao
import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.entity.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberDao: MemberDao,
    private val inviteDao: InviteDao,
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val allInvites: Flow<List<Invite>> = inviteDao.getAllInvites()

    suspend fun createInvite(email: String, role: String) = inviteDao.createInvite(email, role)
}
