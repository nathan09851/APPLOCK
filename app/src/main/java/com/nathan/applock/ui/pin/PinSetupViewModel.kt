package com.nathan.applock.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nathan.applock.data.AppLockerModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class PinSetupStep {
    ENTER_NEW,
    CONFIRM_NEW
}

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER_NEW,
    val firstPin: String = "",
    val currentInput: String = "",
    val errorMessage: String? = null,
    val shakeTrigger: Int = 0,
    val isComplete: Boolean = false,
    val needsOnboarding: Boolean = true
)

class PinSetupViewModel : ViewModel() {
    private val authRepo = AppLockerModule.secureAuthRepository
    private val lockRepo = AppLockerModule.lockRepository

    private val _uiState = MutableStateFlow(PinSetupUiState())
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val complete = lockRepo.onboardingComplete.first()
            _uiState.value = _uiState.value.copy(needsOnboarding = !complete)
        }
    }

    fun onDigitClick(digit: Char) {
        val state = _uiState.value
        if (state.currentInput.length < 8) {
            val nextInput = state.currentInput + digit
            _uiState.value = state.copy(currentInput = nextInput, errorMessage = null)
            
            if (state.step == PinSetupStep.CONFIRM_NEW && nextInput.length == state.firstPin.length) {
                verifyAndSave(nextInput)
            } else if (state.step == PinSetupStep.ENTER_NEW && nextInput.length == 4) {
                _uiState.value = _uiState.value.copy(
                    step = PinSetupStep.CONFIRM_NEW,
                    firstPin = nextInput,
                    currentInput = ""
                )
            }
        }
    }

    fun onBackspaceClick() {
        val state = _uiState.value
        if (state.currentInput.isNotEmpty()) {
            _uiState.value = state.copy(
                currentInput = state.currentInput.dropLast(1),
                errorMessage = null
            )
        } else if (state.step == PinSetupStep.CONFIRM_NEW) {
            _uiState.value = state.copy(
                step = PinSetupStep.ENTER_NEW,
                currentInput = state.firstPin,
                firstPin = "",
                errorMessage = null
            )
        }
    }

    private fun verifyAndSave(confirmPin: String) {
        val state = _uiState.value
        if (confirmPin == state.firstPin) {
            authRepo.setPin(confirmPin)
            _uiState.value = state.copy(isComplete = true)
        } else {
            _uiState.value = state.copy(
                step = PinSetupStep.ENTER_NEW,
                firstPin = "",
                currentInput = "",
                errorMessage = "PINs did not match. Try again.",
                shakeTrigger = state.shakeTrigger + 1
            )
        }
    }
}
