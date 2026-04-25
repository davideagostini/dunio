package com.davideagostini.summ.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.SummTheme

/**
 * Household onboarding screen shown after authentication.
 *
 * Users can create a new shared household or join an existing one before entering the finance
 * area of the app.
 */
@Composable
fun HouseholdSetupScreen(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var householdName by rememberSaveable { mutableStateOf("") }
    var householdId by rememberSaveable { mutableStateOf("") }
    var selectedMode by rememberSaveable { mutableStateOf(HouseholdSetupMode.Create) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    HouseholdSetupContent(
        userName = userName,
        userEmail = userEmail,
        userPhotoUrl = userPhotoUrl,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        selectedMode = selectedMode,
        householdName = householdName,
        householdId = householdId,
        onSelectMode = { selectedMode = it },
        onHouseholdNameChange = { householdName = it },
        onHouseholdIdChange = { householdId = it },
        onDismissError = onDismissError,
        onCreateHousehold = onCreateHousehold,
        onJoinHousehold = onJoinHousehold,
        onSignOut = { showSignOutDialog = true },
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.settings_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    shape = AppButtonShape,
                ) {
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    shape = AppButtonShape,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun HouseholdSetupContent(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    isSubmitting: Boolean,
    errorMessage: String?,
    selectedMode: HouseholdSetupMode,
    householdName: String,
    householdId: String,
    onSelectMode: (HouseholdSetupMode) -> Unit,
    onHouseholdNameChange: (String) -> Unit,
    onHouseholdIdChange: (String) -> Unit,
    onDismissError: () -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AuthHeader(
                    title = stringResource(R.string.auth_household_setup_title),
                    subtitle = stringResource(R.string.auth_household_setup_message),
                )
            }

            item {
                AuthCard {
                    AuthAccountInfoBlock(
                        label = stringResource(R.string.settings_signed_in_as),
                        value = userName,
                        userPhotoUrl = userPhotoUrl,
                    )
                }
            }

            item {
                HouseholdModeCard(
                    selectedMode = selectedMode,
                    onSelectMode = onSelectMode,
                    isSubmitting = isSubmitting,
                    errorMessage = errorMessage,
                    userEmail = userEmail,
                    householdName = householdName,
                    householdId = householdId,
                    onHouseholdNameChange = onHouseholdNameChange,
                    onHouseholdIdChange = onHouseholdIdChange,
                    onDismissError = onDismissError,
                    onCreateHousehold = onCreateHousehold,
                    onJoinHousehold = onJoinHousehold,
                )
            }

            if (!errorMessage.isNullOrBlank() && selectedMode != HouseholdSetupMode.Join) {
                item {
                    AuthErrorCard(errorMessage)
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AppButtonShape,
                ) {
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_9")
@Composable
private fun HouseholdSetupScreenPreview() {
    HouseholdSetupPreviewContainer {
        HouseholdSetupContent(
            userName = "Davide Agostini",
            userEmail = "davide@example.com",
            userPhotoUrl = "https://example.com/avatar.jpg",
            isSubmitting = false,
            errorMessage = null,
            selectedMode = HouseholdSetupMode.Create,
            householdName = "NetWorth D&V",
            householdId = "A1B2C3D4",
            onSelectMode = {},
            onHouseholdNameChange = {},
            onHouseholdIdChange = {},
            onDismissError = {},
            onCreateHousehold = {},
            onJoinHousehold = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_9")
@Composable
private fun HouseholdSetupScreenErrorPreview() {
    HouseholdSetupPreviewContainer {
        HouseholdSetupContent(
            userName = "Davide Agostini",
            userEmail = "davide@example.com",
            userPhotoUrl = "https://example.com/avatar.jpg",
            isSubmitting = false,
            errorMessage = "No pending invite was found for this Google account. Make sure the invite uses the exact email you are signed in with.",
            selectedMode = HouseholdSetupMode.Join,
            householdName = "",
            householdId = "ZZZZ9999",
            onSelectMode = {},
            onHouseholdNameChange = {},
            onHouseholdIdChange = {},
            onDismissError = {},
            onCreateHousehold = {},
            onJoinHousehold = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_9")
@Composable
private fun HouseholdSetupScreenSubmittingPreview() {
    HouseholdSetupPreviewContainer {
        HouseholdSetupContent(
            userName = "Davide Agostini",
            userEmail = "davide@example.com",
            userPhotoUrl = "https://example.com/avatar.jpg",
            isSubmitting = true,
            errorMessage = null,
            selectedMode = HouseholdSetupMode.Create,
            householdName = "Summ Home",
            householdId = "",
            onSelectMode = {},
            onHouseholdNameChange = {},
            onHouseholdIdChange = {},
            onDismissError = {},
            onCreateHousehold = {},
            onJoinHousehold = {},
            onSignOut = {},
        )
    }
}

@Composable
private fun HouseholdSetupPreviewContainer(
    content: @Composable () -> Unit,
) {
    SummTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            content()
        }
    }
}

@Composable
private fun HouseholdActionCard(
    title: String,
    message: String,
    fieldValue: String,
    fieldLabel: String,
    onFieldValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    actionLabel: String,
    keyboardCapitalization: KeyboardCapitalization,
    inlineErrorMessage: String? = null,
    signedInEmail: String? = null,
) {
    AuthCard {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (keyboardCapitalization == KeyboardCapitalization.None) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_join_household_helper),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!signedInEmail.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.auth_join_household_signed_in_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = signedInEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.auth_join_household_signed_in_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!inlineErrorMessage.isNullOrBlank()) {
            AuthErrorCard(inlineErrorMessage)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = onFieldValueChange,
            label = { Text(fieldLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = keyboardCapitalization),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = AppButtonShape,
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun HouseholdModeCard(
    selectedMode: HouseholdSetupMode,
    onSelectMode: (HouseholdSetupMode) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    userEmail: String,
    householdName: String,
    householdId: String,
    onHouseholdNameChange: (String) -> Unit,
    onHouseholdIdChange: (String) -> Unit,
    onDismissError: () -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
) {
    AuthCard {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HouseholdSetupMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = HouseholdSetupMode.entries.size,
                    ),
                    onClick = { onSelectMode(mode) },
                    enabled = !isSubmitting,
                    selected = selectedMode == mode,
                    label = {
                        Text(
                            text = stringResource(mode.labelRes),
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedMode) {
            HouseholdSetupMode.Create -> HouseholdActionCard(
                title = stringResource(R.string.auth_create_household_title),
                message = stringResource(R.string.auth_create_household_message),
                fieldValue = householdName,
                fieldLabel = stringResource(R.string.auth_household_name_label),
                onFieldValueChange = onHouseholdNameChange,
                onSubmit = {
                    onDismissError()
                    onCreateHousehold(householdName)
                },
                enabled = !isSubmitting && householdName.isNotBlank(),
                actionLabel = stringResource(R.string.auth_create_household_action),
                keyboardCapitalization = KeyboardCapitalization.Words,
                inlineErrorMessage = null,
            )

            HouseholdSetupMode.Join -> HouseholdActionCard(
                title = stringResource(R.string.auth_join_household_title),
                message = stringResource(R.string.auth_join_household_message),
                fieldValue = householdId,
                fieldLabel = stringResource(R.string.auth_household_id_label),
                onFieldValueChange = onHouseholdIdChange,
                onSubmit = {
                    onDismissError()
                    onJoinHousehold(householdId)
                },
                enabled = !isSubmitting && householdId.isNotBlank(),
                actionLabel = stringResource(R.string.auth_join_household_action),
                keyboardCapitalization = KeyboardCapitalization.None,
                inlineErrorMessage = errorMessage,
                signedInEmail = userEmail,
            )
        }
    }
}

private enum class HouseholdSetupMode(val labelRes: Int) {
    Create(R.string.auth_create_short),
    Join(R.string.auth_join_short),
}
