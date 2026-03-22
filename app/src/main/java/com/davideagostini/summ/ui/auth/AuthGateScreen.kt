package com.davideagostini.summ.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.theme.SummColors

@Composable
fun AuthGateScreen(
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = sessionState) {
        SessionState.Loading -> LoadingScreen()
        is SessionState.ConfigurationError -> MessageScreen(
            title = stringResource(R.string.auth_configuration_required),
            message = state.message,
        )
        SessionState.SignedOut -> SignInScreen(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismissError = viewModel::consumeError,
            onGoogleSignIn = {
                viewModel.signInWithGoogle(context)
            },
        )
        is SessionState.NeedsHousehold -> HouseholdSetupScreen(
            userName = state.user.name,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismissError = viewModel::consumeError,
            onCreateHousehold = viewModel::createHousehold,
            onJoinHousehold = viewModel::joinHousehold,
            onSignOut = viewModel::signOut,
        )
        is SessionState.Ready -> Unit
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            colors = SummColors.topBarColors,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.auth_sign_in_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.auth_sign_in_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            onDismissError()
                            onGoogleSignIn()
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isSubmitting) stringResource(R.string.auth_signing_in) else stringResource(R.string.auth_continue_with_google))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdSetupScreen(
    userName: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var householdName by rememberSaveable { mutableStateOf("") }
    var householdId by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.auth_household_setup_title), fontWeight = FontWeight.Bold) },
            colors = SummColors.topBarColors,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.auth_signed_in_as, userName), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.auth_create_household_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = householdName,
                            onValueChange = { householdName = it },
                            label = { Text(stringResource(R.string.auth_household_name_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                onDismissError()
                                onCreateHousehold(householdName)
                            },
                            enabled = !isSubmitting && householdName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.auth_create_household_action))
                        }
                    }
                }
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.auth_join_household_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = householdId,
                            onValueChange = { householdId = it },
                            label = { Text(stringResource(R.string.auth_household_id_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                onDismissError()
                                onJoinHousehold(householdId)
                            },
                            enabled = !isSubmitting && householdId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.auth_join_household_action))
                        }
                    }
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                item {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = onSignOut,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageScreen(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            colors = SummColors.topBarColors,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
