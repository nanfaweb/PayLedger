package com.payledger.app.data.repository

import com.payledger.app.data.model.CreateTransactionRequest
import com.payledger.app.data.model.DashboardSummary
import com.payledger.app.data.model.Transaction
import com.payledger.app.data.model.TransactionPage
import com.payledger.app.data.remote.ApiService

class TransactionRepository(private val api: ApiService) {
    suspend fun summary(): DashboardSummary = api.summary()

    suspend fun search(filters: Map<String, String>): TransactionPage = api.transactions(filters)

    suspend fun get(id: String): Transaction = api.getTransaction(id)

    suspend fun create(request: CreateTransactionRequest): Transaction = api.createTransaction(request)

    suspend fun update(id: String, patch: Map<String, Any?>): Transaction = api.updateTransaction(id, patch)

    suspend fun delete(id: String) {
        api.deleteTransaction(id)
    }

    suspend fun updateProvider(provider: String, enabled: Boolean) {
        api.updateProvider(mapOf("provider" to provider, "monitoring_enabled" to enabled))
    }
}
