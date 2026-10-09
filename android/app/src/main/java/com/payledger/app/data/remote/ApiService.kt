package com.payledger.app.data.remote

import com.payledger.app.data.model.AuthResponse
import com.payledger.app.data.model.CreateTransactionRequest
import com.payledger.app.data.model.DashboardSummary
import com.payledger.app.data.model.LicenseStatus
import com.payledger.app.data.model.MeResponse
import com.payledger.app.data.model.Transaction
import com.payledger.app.data.model.TransactionPage
import com.payledger.app.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Response<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<Unit>

    @GET("me")
    suspend fun me(): MeResponse

    @PATCH("me")
    suspend fun updateProfile(@Body body: Map<String, String>): Map<String, User>

    @DELETE("me")
    suspend fun deleteAccount(): Response<Unit>

    @PUT("me/providers")
    suspend fun updateProvider(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, List<com.payledger.app.data.model.ProviderSetting>>

    @GET("license/status")
    suspend fun licenseStatus(): LicenseStatus

    @POST("license/activate")
    suspend fun activate(@Body body: Map<String, String>): LicenseStatus

    @GET("transactions/summary")
    suspend fun summary(): DashboardSummary

    @GET("transactions")
    suspend fun transactions(@QueryMap options: Map<String, String>): TransactionPage

    @POST("transactions")
    suspend fun createTransaction(@Body body: CreateTransactionRequest): Transaction

    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") id: String): Transaction

    @PATCH("transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Transaction

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>
}
