package com.nathan.applock.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nathan.applock.data.AppLockerModule
import com.nathan.applock.data.RelockPolicy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val biometricEnabled: Boolean = true,
    val relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATELY,
    val scrambleKeypad: Boolean = false,
    val disguiseCrash: Boolean = true,
    val intruderLogs: List<String> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val lockRepo = AppLockerModule.lockRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        lockRepo.biometricEnabled,
        lockRepo.relockPolicy,
        lockRepo.scrambleKeypad,
        lockRepo.disguiseCrash,
        lockRepo.intruderLogs
    ) { bio, policy, scramble, disguise, logs ->
        SettingsUiState(
            biometricEnabled = bio,
            relockPolicy = policy,
            scrambleKeypad = scramble,
            disguiseCrash = disguise,
            intruderLogs = logs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            lockRepo.setBiometricEnabled(enabled)
        }
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        viewModelScope.launch {
            lockRepo.setRelockPolicy(policy)
        }
    }

    fun setScrambleKeypad(scramble: Boolean) {
        viewModelScope.launch {
            lockRepo.setScrambleKeypad(scramble)
        }
    }

    fun setDisguiseCrash(disguise: Boolean) {
        viewModelScope.launch {
            lockRepo.setDisguiseCrash(disguise)
        }
    }

    fun clearIntruderLogs() {
        viewModelScope.launch {
            lockRepo.clearIntruderLogs()
        }
    }
}
