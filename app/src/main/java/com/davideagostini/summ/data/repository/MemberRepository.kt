package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.InviteDao
import com.davideagostini.summ.data.dao.MemberDao
import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.entity.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Repository facade for household members.
 *
 * The members feature uses this layer to observe the household roster and perform invite/member
 * actions through a narrow API.
 */
class MemberRepository @Inject constructor(
    private val memberDao: MemberDao,
    private val inviteDao: InviteDao,
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val allInvites: Flow<List<Invite>> = inviteDao.getAllInvites()

    suspend fun createInvite(email: String, role: String) = inviteDao.createInvite(email, role)
}
