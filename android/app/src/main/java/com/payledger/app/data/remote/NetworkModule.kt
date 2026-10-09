package com.payledger.app.data.remote

import com.payledger.app.BuildConfig
import com.payledger.app.data.local.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshClient: OkHttpClient,
    private val moshi: Moshi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val refresh = tokenStore.refreshToken ?: return null
        val body = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/json"),
            """{"refresh_token":"$refresh"}"""
        )
        val refreshRequest = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "auth/refresh")
            .post(body)
            .build()
        val refreshResponse = refreshClient.newCall(refreshRequest).execute()
        if (!refreshResponse.isSuccessful) {
            tokenStore.clearSession()
            return null
        }
        val json = refreshResponse.body()?.string().orEmpty()
        val adapter = moshi.adapter(com.payledger.app.data.model.AuthResponse::class.java)
        val auth = adapter.fromJson(json) ?: return null
        tokenStore.saveSession(
            auth.access_token,
            auth.refresh_token,
            auth.user.id,
            auth.user.full_name,
            auth.user.phone_number,
            auth.user.preferred_language,
        )
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${auth.access_token}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

object NetworkModule {
    fun createApi(tokenStore: TokenStore): ApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.accessToken
            val req = if (!token.isNullOrBlank()) {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            chain.proceed(req)
        }

        val plainClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(tokenStore, plainClient, moshi))
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
