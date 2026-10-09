package com.payledger.app.data.repository

import com.payledger.app.data.model.LicenseStatus
import com.payledger.app.data.remote.ApiService

class LicenseRepository(private val api: ApiService) {
    suspend fun status(): LicenseStatus = api.licenseStatus()
    suspend fun activate(code: String): LicenseStatus = api.activate(mapOf("code" to code.trim()))
}
