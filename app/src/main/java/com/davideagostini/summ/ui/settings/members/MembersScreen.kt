package com.davideagostini.summ.ui.settings.members

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.entity.Member
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.SummColors
import com.davideagostini.summ.ui.theme.listItemShape
import kotlinx.coroutines.launch

/**
 * Household members screen.
 *
 * It combines the current roster and invite management UI inside one mobile-first settings flow.
 */
@Composable
fun MembersScreen(
    householdId: String,
    currentUserId: String,
    currentUserEmail: String,
    onBack: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val invites by viewModel.invites.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    MembersContent(
        householdId = householdId,
        currentUserId = currentUserId,
        currentUserEmail = currentUserEmail,
        members = members,
        invites = invites,
        uiState = uiState,
        onBack = onBack,
        onUpdateInviteEmail = viewModel::updateInviteEmail,
        onUpdateInviteRole = viewModel::updateInviteRole,
        onSaveInvite = viewModel::saveInvite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersContent(
    householdId: String,
    currentUserId: String,
    currentUserEmail: String,
    members: List<Member>,
    invites: List<Invite>,
    uiState: MembersUiState,
    onBack: () -> Unit,
    onUpdateInviteEmail: (String) -> Unit,
    onUpdateInviteRole: (String) -> Unit,
    onSaveInvite: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val signedInRole = members.firstOrNull { it.userId == currentUserId }?.role ?: "member"
    val isOwner = signedInRole == "owner"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.members_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back),
                    )
                }
            },
            colors = SummColors.topBarColors,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MembersHeaderCard(
                    householdId = householdId,
                    // Copying the household id uses the new suspend clipboard API so we can avoid
                    // the deprecated LocalClipboardManager path.
                    onCopyHouseholdId = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipData.newPlainText("household_id", householdId).toClipEntry())
                        }
                    },
                )
            }

            item { SectionLabel(stringResource(R.string.members_list_title)) }

            itemsIndexed(members, key = { _, member -> member.userId }) { index, member ->
                MemberRow(
                    member = member,
                    currentUserEmail = currentUserEmail,
                    isCurrentUser = member.userId == currentUserId,
                    index = index,
                    count = members.size,
                )
            }

            item {
                if (isOwner) {
                    InviteCard(
                        uiState = uiState,
                        signedInRole = signedInRole,
                        onUpdateInviteEmail = onUpdateInviteEmail,
                        onUpdateInviteRole = onUpdateInviteRole,
                        onSaveInvite = onSaveInvite,
                    )
                } else {
                    ReadOnlyAccessCard(
                        signedInRole = signedInRole,
                    )
                }
            }

            if (isOwner) {
                item { SectionLabel(stringResource(R.string.members_pending_invites_title)) }
            }

            if (isOwner && invites.isEmpty()) {
                item {
                    EmptyInfoCard(stringResource(R.string.members_no_invites))
                }
            } else if (isOwner) {
                itemsIndexed(invites, key = { _, invite -> invite.id }) { index, invite ->
                    InviteRow(invite = invite, index = index, count = invites.size)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyAccessCard(
    signedInRole: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.members_access_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.members_access_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SignedInRoleBadge(signedInRole = signedInRole)
        }
    }
}

@Composable
private fun MembersHeaderCard(
    householdId: String,
    onCopyHouseholdId: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.members_household_members_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.members_household_members_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onCopyHouseholdId, shape = AppButtonShape) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.members_copy_id))
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = stringResource(R.string.members_household_id_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = householdId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: Member,
    currentUserEmail: String,
    isCurrentUser: Boolean,
    index: Int,
    count: Int,
) {
    val verticalPadding = when {
        count == 1 -> PaddingValues(vertical = 4.dp)
        index == 0 -> PaddingValues(top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(top = 1.dp, bottom = 4.dp)
        else -> PaddingValues(vertical = 1.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(verticalPadding),
        shape = listItemShape(index, count),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MemberAvatar(member = member)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name.ifBlank { member.email.ifBlank { member.userId } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = member.email.ifBlank {
                        if (isCurrentUser && currentUserEmail.isNotBlank()) currentUserEmail
                        else stringResource(R.string.members_no_email)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = roleLabel(member.role),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InviteCard(
    uiState: MembersUiState,
    signedInRole: String,
    onUpdateInviteEmail: (String) -> Unit,
    onUpdateInviteRole: (String) -> Unit,
    onSaveInvite: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.members_invite_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.members_invite_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = uiState.inviteEmail,
                onValueChange = onUpdateInviteEmail,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.members_invite_email_label)) },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { message -> { Text(message) } },
                shape = RoundedCornerShape(16.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.members_role_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("member" to stringResource(R.string.members_role_member), "owner" to stringResource(R.string.members_role_owner)).forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = uiState.inviteRole == item.first,
                            onClick = { onUpdateInviteRole(item.first) },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(item.second)
                        }
                    }
                }
            }

            SignedInRoleBadge(signedInRole = signedInRole)

            Button(
                onClick = onSaveInvite,
                modifier = Modifier.fillMaxWidth(),
                shape = AppButtonShape,
            ) {
                Text(stringResource(R.string.members_save_invite))
            }
        }
    }
}

@Composable
private fun SignedInRoleBadge(
    signedInRole: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.members_signed_in_role, roleLabel(signedInRole)),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun roleLabel(role: String): String =
    when (role) {
        "owner" -> stringResource(R.string.members_role_owner)
        "member" -> stringResource(R.string.members_role_member)
        else -> role.replaceFirstChar { it.uppercase() }
    }

@Composable
private fun InviteRow(
    invite: Invite,
    index: Int,
    count: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = listItemShape(index, count),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = invite.email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = invite.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyInfoCard(message: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp),
    )
}

@Composable
private fun MemberAvatar(member: Member) {
    val initials = member.name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank {
            member.email.take(1).uppercase().ifBlank { "M" }
        }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
