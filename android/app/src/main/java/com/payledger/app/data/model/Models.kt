package com.payledger.app.data.model

data class User(
    val id: String,
    val full_name: String,
    val phone_number: String,
    val preferred_language: String = "en",
)

data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "Bearer",
    val expires_in: Int = 900,
    val user: User,
)

data class LicenseStatus(
    val active: Boolean,
    val status: String,
    val activated_at: String? = null,
    val expires_at: String? = null,
    val days_remaining: Int = 0,
    val last_renewed_at: String? = null,
)

data class Transaction(
    val id: String,
    val provider: String,
    val sender_name: String? = null,
    val sender_identifier: String? = null,
    val amount: Double,
    val currency: String = "PKR",
    val transaction_reference: String? = null,
    val transaction_timestamp: String,
    val received_at: String? = null,
    val transaction_direction: String = "incoming",
    val original_notification: String? = null,
    val source_type: String = "notification",
    val notes: String? = null,
    val review_status: String = "confirmed",
    val deduplication_key: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

data class TransactionPage(
    val items: List<Transaction>,
    val page: Int,
    val page_size: Int,
    val total: Int,
    val total_pages: Int,
)

data class ProviderTotal(
    val provider: String,
    val total: Double,
    val count: Int,
)

data class DashboardSummary(
    val today_total: Double,
    val week_total: Double,
    val month_total: Double,
    val transaction_count: Int,
    val currency: String = "PKR",
    val by_provider: List<ProviderTotal> = emptyList(),
    val recent: List<Transaction> = emptyList(),
)

data class ProviderSetting(
    val provider: String,
    val monitoring_enabled: Boolean,
)

data class LastNotification(
    val provider: String? = null,
    val processing_status: String? = null,
    val received_at: String? = null,
)

data class MeResponse(
    val user: User,
    val license: LicenseStatus,
    val providers: List<ProviderSetting> = emptyList(),
    val last_notification: LastNotification? = null,
)

data class CreateTransactionRequest(
    val provider: String,
    val sender_name: String? = null,
    val sender_identifier: String? = null,
    val amount: Double,
    val currency: String = "PKR",
    val transaction_reference: String? = null,
    val transaction_timestamp: String? = null,
    val transaction_direction: String = "incoming",
    val original_notification: String? = null,
    val source_type: String = "manual",
    val notes: String? = null,
    val review_status: String? = null,
    val deduplication_key: String? = null,
)

data class ApiErrorBody(
    val error: String? = null,
    val code: String? = null,
)
