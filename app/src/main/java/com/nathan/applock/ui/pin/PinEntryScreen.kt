package com.nathan.applock.ui.pin

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PinEntryScreen(
    targetPackageName: String? = null,
    onUnlockSuccess: () -> Unit,
    viewModel: PinEntryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(targetPackageName) {
        viewModel.initForPackage(targetPackageName)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onUnlockSuccess()
        }
    }

    LaunchedEffect(uiState.autoTriggerBiometric) {
        if (uiState.autoTriggerBiometric) {
            viewModel.onBiometricTriggered()
            showBiometricPrompt(context, viewModel::onBiometricSuccess)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.appIcon != null) {
                    Image(
                        bitmap = uiState.appIcon!!,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = if (uiState.appLabel != null) "Unlock ${uiState.appLabel}" else "Enter PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLockedOut) {
                    Text(
                        text = "Try again in ${uiState.lockoutRemainingSeconds}s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Enter your PIN or use biometrics",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                PinDots(
                    pinLength = uiState.currentInput.length,
                    maxDots = 4,
                    shakeTrigger = uiState.shakeTrigger
                )
            }

            PinKeypad(
                onDigitClick = viewModel::onDigitClick,
                onBackspaceClick = viewModel::onBackspaceClick,
                showBiometric = uiState.canUseBiometric,
                onBiometricClick = { showBiometricPrompt(context, viewModel::onBiometricSuccess) },
                scramble = uiState.scrambleKeypad,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

private fun showBiometricPrompt(context: Context, onSuccess: () -> Unit) {
    var currentContext = context
    var activity: FragmentActivity? = null
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            activity = currentContext
            break
        }
        currentContext = currentContext.baseContext
    }

    if (activity == null) return

    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock App")
        .setSubtitle("Authenticate to continue")
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText("Use PIN")
        .build()

    try {
        prompt.authenticate(promptInfo)
    } catch (_: Exception) {}
}
