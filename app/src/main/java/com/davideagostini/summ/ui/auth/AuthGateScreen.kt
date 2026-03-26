package com.davideagostini.summ.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.auth.components.HouseholdSetupScreen
import com.davideagostini.summ.ui.auth.components.MessageCard
import com.davideagostini.summ.ui.auth.components.SignInScreen

@Composable
fun AuthGateScreen(
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isSubmitting && uiState.errorMessage == null && sessionState !is SessionState.Ready) {
        LoadingScreen()
        return
    }

    if (uiState.isHouseholdTransitioning && uiState.errorMessage == null) {
        LoadingScreen()
        return
    }

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
            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
        )
        is SessionState.NeedsHousehold -> HouseholdSetupScreen(
            userName = state.user.name,
            userPhotoUrl = state.user.photoUrl,
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

@Composable
private fun MessageScreen(
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        MessageCard(
            title = title,
            message = message,
        )
    }
}
