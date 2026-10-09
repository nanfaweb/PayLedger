package com.payledger.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "payledger_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    var phoneNumber: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANG, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    var welcomeSeen: Boolean
        get() = prefs.getBoolean(KEY_WELCOME, false)
        set(value) = prefs.edit().putBoolean(KEY_WELCOME, value).apply()

    fun saveSession(access: String, refresh: String, userId: String, fullName: String, phone: String, language: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE, phone)
            .putString(KEY_LANG, language)
            .apply()
    }

    fun clearSession() {
        val lang = language
        val welcome = welcomeSeen
        prefs.edit().clear().apply()
        language = lang
        welcomeSeen = welcome
    }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PHONE = "phone"
        private const val KEY_LANG = "language"
        private const val KEY_WELCOME = "welcome_seen"
    }
}
