package com.nathan.applock.ui.pin

import android.app.Application
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nathan.applock.data.AppLockerModule
import com.nathan.applock.util.toImageBitmapSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PinEntryUiState(
    val currentInput: String = "",
    val errorMessage: String? = null,
    val shakeTrigger: Int = 0,
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val isSuccess: Boolean = false,
    val appLabel: String? = null,
    val appIcon: ImageBitmap? = null,
    val canUseBiometric: Boolean = false,
    val autoTriggerBiometric: Boolean = false,
    val scrambleKeypad: Boolean = false
)

class PinEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AppLockerModule.secureAuthRepository
    private val lockRepo = AppLockerModule.lockRepository

    private val _uiState = MutableStateFlow(PinEntryUiState())
    val uiState: StateFlow<PinEntryUiState> = _uiState.asStateFlow()

    private var targetPackageName: String? = null

    fun initForPackage(packageName: String?) {
        if (targetPackageName == packageName && packageName != null) return
        targetPackageName = packageName

        viewModelScope.launch {
            val enabled = lockRepo.biometricEnabled.first()
            val scramble = lockRepo.scrambleKeypad.first()
            val bm = BiometricManager.from(getApplication())
            val canAuth = bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val useBio = enabled && canAuth

            var label: String? = null
            var icon: ImageBitmap? = null

            if (packageName != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val pm = getApplication<Application>().packageManager
                        val info = pm.getApplicationInfo(packageName, 0)
                        label = pm.getApplicationLabel(info).toString()
                        icon = pm.getApplicationIcon(info).toImageBitmapSafe()
                    } catch (_: Exception) {}
                }
            }

            _uiState.value = _uiState.value.copy(
                appLabel = label,
                appIcon = icon,
                canUseBiometric = useBio,
                autoTriggerBiometric = useBio,
                scrambleKeypad = scramble
            )
        }
    }

    fun onBiometricTriggered() {
        _uiState.value = _uiState.value.copy(autoTriggerBiometric = false)
    }

    fun onBiometricSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = true, errorMessage = null)
    }

    fun onDigitClick(digit: Char) {
        val state = _uiState.value
        if (state.isLockedOut || state.isSuccess) return

        if (state.currentInput.length < 8) {
            val nextInput = state.currentInput + digit
            _uiState.value = state.copy(currentInput = nextInput, errorMessage = null)

            if (nextInput.length == authRepo.pinLength) {
                if (authRepo.verifyPin(nextInput)) {
                    _uiState.value = _uiState.value.copy(isSuccess = true, errorMessage = null)
                } else {
                    handleFailedAttempt()
                }
            }
        }
    }

    fun submitPin() {
        val state = _uiState.value
        if (state.isLockedOut || state.isSuccess || state.currentInput.isEmpty()) return

        if (authRepo.verifyPin(state.currentInput)) {
            _uiState.value = state.copy(isSuccess = true, errorMessage = null)
        } else {
            handleFailedAttempt()
        }
    }

    fun onBackspaceClick() {
        val state = _uiState.value
        if (state.isLockedOut || state.isSuccess) return

        if (state.currentInput.isNotEmpty()) {
            _uiState.value = state.copy(
                currentInput = state.currentInput.dropLast(1),
                errorMessage = null
            )
        }
    }

    private fun handleFailedAttempt() {
        val state = _uiState.value
        val newAttempts = state.failedAttempts + 1
        if (newAttempts >= 3) {
            viewModelScope.launch {
                lockRepo.logIntruderAttempt(targetPackageName ?: state.appLabel ?: "Unknown App")
            }
        }
        if (newAttempts >= 5) {
            startLockout()
        } else {
            _uiState.value = state.copy(
                currentInput = "",
                errorMessage = "Incorrect PIN ($newAttempts/5 attempts)",
                shakeTrigger = state.shakeTrigger + 1,
                failedAttempts = newAttempts
            )
        }
    }

    private fun startLockout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentInput = "",
                errorMessage = "Too many failed attempts. Locked out.",
                shakeTrigger = _uiState.value.shakeTrigger + 1,
                failedAttempts = 5,
                isLockedOut = true,
                lockoutRemainingSeconds = 30
            )

            for (i in 30 downTo 1) {
                _uiState.value = _uiState.value.copy(lockoutRemainingSeconds = i)
                delay(1000L)
            }

            _uiState.value = _uiState.value.copy(
                isLockedOut = false,
                failedAttempts = 0,
                lockoutRemainingSeconds = 0,
                errorMessage = null
            )
        }
    }
}
