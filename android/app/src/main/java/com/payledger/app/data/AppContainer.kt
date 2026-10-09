package com.payledger.app.data

import android.content.Context
import com.payledger.app.data.local.ProviderPrefs
import com.payledger.app.data.local.TokenStore
import com.payledger.app.data.remote.NetworkModule
import com.payledger.app.data.repository.AuthRepository
import com.payledger.app.data.repository.LicenseRepository
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.notification.NotificationIngestBus

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)
    val providerPrefs = ProviderPrefs(context)
    val api = NetworkModule.createApi(tokenStore)
    val authRepository = AuthRepository(api, tokenStore)
    val licenseRepository = LicenseRepository(api)
    val transactionRepository = TransactionRepository(api)
    val notificationBus = NotificationIngestBus
}
