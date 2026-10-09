package com.payledger.app.data.local

import android.content.Context

/** Local mirror of provider monitoring toggles (also synced to server when online). */
class ProviderPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("payledger_providers", Context.MODE_PRIVATE)

    fun isEnabled(provider: String): Boolean = prefs.getBoolean(provider, true)

    fun setEnabled(provider: String, enabled: Boolean) {
        prefs.edit().putBoolean(provider, enabled).apply()
    }

    fun all(): Map<String, Boolean> = PROVIDERS.associateWith { isEnabled(it) }

    companion object {
        val PROVIDERS = listOf("jazzcash", "easypaisa", "sadapay", "nayapay")
    }
}
