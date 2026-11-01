package com.example.extreapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.extreapp.data.local.entity.LockTimeout

interface SecurityPreferences {
    var isPrivacyModeEnabled: Boolean
    var isBiometricLockEnabled: Boolean
    var isScreenProtectionEnabled: Boolean
    var lockTimeout: LockTimeout
}

class SharedPreferencesSecurityPreferences(context: Context) : SecurityPreferences {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override var isPrivacyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_MODE, value).apply()

    override var isBiometricLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply()

    override var isScreenProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_PROTECTION, value).apply()

    override var lockTimeout: LockTimeout
        get() = LockTimeout.fromName(prefs.getString(KEY_LOCK_TIMEOUT, LockTimeout.IMMEDIATE.name))
        set(value) = prefs.edit().putString(KEY_LOCK_TIMEOUT, value.name).apply()

    companion object {
        private const val PREFS_NAME = "extreapp_security_preferences"
        private const val KEY_PRIVACY_MODE = "key_privacy_mode_enabled"
        private const val KEY_BIOMETRIC_LOCK = "key_biometric_lock_enabled"
        private const val KEY_SCREEN_PROTECTION = "key_screen_protection_enabled"
        private const val KEY_LOCK_TIMEOUT = "key_lock_timeout"
    }
}

class InMemorySecurityPreferences(
    override var isPrivacyModeEnabled: Boolean = false,
    override var isBiometricLockEnabled: Boolean = false,
    override var isScreenProtectionEnabled: Boolean = true,
    override var lockTimeout: LockTimeout = LockTimeout.IMMEDIATE
) : SecurityPreferences

