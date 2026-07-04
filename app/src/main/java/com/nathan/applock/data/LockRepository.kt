package com.nathan.applock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "applock_settings")

class LockRepository(private val context: Context) {

    private object Keys {
        val LOCKED_PACKAGES = stringSetPreferencesKey("locked_packages")
        val RELOCK_POLICY = stringPreferencesKey("relock_policy")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val SCRAMBLE_KEYPAD = booleanPreferencesKey("scramble_keypad")
        val DISGUISE_CRASH = booleanPreferencesKey("disguise_crash")
        val INTRUDER_LOGS = stringSetPreferencesKey("intruder_logs")
    }

    // --- Locked packages ---

    val lockedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOCKED_PACKAGES] ?: emptySet()
    }

    suspend fun setPackageLocked(packageName: String, locked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.LOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (locked) current.add(packageName) else current.remove(packageName)
            prefs[Keys.LOCKED_PACKAGES] = current
        }
    }

    // --- Relock policy ---

    val relockPolicy: Flow<RelockPolicy> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.RELOCK_POLICY] ?: RelockPolicy.IMMEDIATELY.name
        try { RelockPolicy.valueOf(name) } catch (_: Exception) { RelockPolicy.IMMEDIATELY }
    }

    suspend fun setRelockPolicy(policy: RelockPolicy) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RELOCK_POLICY] = policy.name
        }
    }

    // --- Biometric ---

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: true
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    // --- Onboarding ---

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    // --- Hidden packages (Vault) ---

    val hiddenPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIDDEN_PACKAGES] ?: emptySet()
    }

    suspend fun setPackageHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val hiddenSet = prefs[Keys.HIDDEN_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            val lockedSet = prefs[Keys.LOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (hidden) {
                hiddenSet.add(packageName)
                lockedSet.add(packageName) // Hidden packages must also be locked
            } else {
                hiddenSet.remove(packageName)
            }
            prefs[Keys.HIDDEN_PACKAGES] = hiddenSet
            prefs[Keys.LOCKED_PACKAGES] = lockedSet
        }
    }

    // --- Security Customizations ---

    val scrambleKeypad: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLE_KEYPAD] ?: false
    }

    suspend fun setScrambleKeypad(scramble: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCRAMBLE_KEYPAD] = scramble
        }
    }

    val disguiseCrash: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISGUISE_CRASH] ?: true
    }

    suspend fun setDisguiseCrash(disguise: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISGUISE_CRASH] = disguise
        }
    }

    // --- Intruder Alerts Log ---

    val intruderLogs: Flow<List<String>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.INTRUDER_LOGS] ?: emptySet()).toList().sortedDescending()
    }

    suspend fun logIntruderAttempt(packageName: String) {
        val timestamp = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "$timestamp - Failed attempt on $packageName"
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.INTRUDER_LOGS]?.toMutableSet() ?: mutableSetOf()
            current.add(entry)
            if (current.size > 20) {
                val sorted = current.sortedDescending().take(20)
                current.clear()
                current.addAll(sorted)
            }
            prefs[Keys.INTRUDER_LOGS] = current
        }
    }

    suspend fun clearIntruderLogs() {
        context.dataStore.edit { prefs ->
            prefs[Keys.INTRUDER_LOGS] = emptySet()
        }
    }
}
