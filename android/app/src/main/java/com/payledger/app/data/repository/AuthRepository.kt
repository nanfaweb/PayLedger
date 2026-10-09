package com.payledger.app.data.repository

import com.payledger.app.data.local.TokenStore
import com.payledger.app.data.model.AuthResponse
import com.payledger.app.data.model.MeResponse
import com.payledger.app.data.remote.ApiService

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    suspend fun register(fullName: String, phone: String, password: String, confirm: String): AuthResponse {
        val res = api.register(
            mapOf(
                "full_name" to fullName,
                "phone_number" to phone,
                "password" to password,
                "confirm_password" to confirm,
            )
        )
        persist(res)
        return res
    }

    suspend fun login(phone: String, password: String): AuthResponse {
        val res = api.login(mapOf("phone_number" to phone, "password" to password))
        persist(res)
        return res
    }

    suspend fun resetPassword(phone: String, token: String, newPassword: String, confirm: String) {
        api.resetPassword(
            mapOf(
                "phone_number" to phone,
                "reset_token" to token,
                "new_password" to newPassword,
                "confirm_password" to confirm,
            )
        )
    }

    suspend fun logout() {
        val refresh = tokenStore.refreshToken
        runCatching {
            if (!refresh.isNullOrBlank()) api.logout(mapOf("refresh_token" to refresh))
        }
        tokenStore.clearSession()
    }

    suspend fun me(): MeResponse = api.me()

    suspend fun updateLanguage(lang: String) {
        api.updateProfile(mapOf("preferred_language" to lang))
        tokenStore.language = lang
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
        tokenStore.clearSession()
    }

    private fun persist(res: AuthResponse) {
        tokenStore.saveSession(
            res.access_token,
            res.refresh_token,
            res.user.id,
            res.user.full_name,
            res.user.phone_number,
            res.user.preferred_language,
        )
    }
}
