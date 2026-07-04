package com.nathan.applock.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class SecureAuthRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Throwable) {
            // If Keystore encryption fails (common on Samsung devices after APK update or backup restore),
            // clear corrupted prefs file and retry once.
            try {
                context.deleteSharedPreferences("applock_secure_auth")
            } catch (_: Throwable) {}
            try {
                createEncryptedPrefs()
            } catch (fallbackEx: Throwable) {
                // If hardware Keystore is permanently broken, fallback to standard SharedPreferences
                context.getSharedPreferences("applock_secure_auth_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "applock_secure_auth",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private companion object {
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_SET_UP = "pin_set_up"
        const val KEY_PIN_LENGTH = "pin_length"
    }

    val isPinSetUp: Boolean
        get() = try {
            prefs.getBoolean(KEY_PIN_SET_UP, false)
        } catch (_: Throwable) {
            false
        }

    val pinLength: Int
        get() = try {
            prefs.getInt(KEY_PIN_LENGTH, 4)
        } catch (_: Throwable) {
            4
        }

    fun setPin(pin: String) {
        try {
            val salt = generateSalt()
            val hash = hashPin(pin, salt)
            prefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putString(KEY_PIN_SALT, salt)
                .putBoolean(KEY_PIN_SET_UP, true)
                .putInt(KEY_PIN_LENGTH, pin.length)
                .apply()
        } catch (_: Throwable) {}
    }

    fun verifyPin(pin: String): Boolean {
        return try {
            val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
            val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
            hashPin(pin, storedSalt) == storedHash
        } catch (_: Throwable) {
            false
        }
    }

    private fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        digest.update(saltBytes)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}

