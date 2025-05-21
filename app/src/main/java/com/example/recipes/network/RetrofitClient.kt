// app/src/main/java/com/example/recipes/network/RetrofitClient.kt
package com.example.recipes.network

import com.example.recipes.BuildConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.spoonacular.com/"

    // 1) Logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    // 2) API‐key interceptor
    private val apiKeyInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val urlWithKey = original.url
            .newBuilder()
            .addQueryParameter("apiKey", BuildConfig.SPOONACULAR_API_KEY)
            .build()
        val keyed = original.newBuilder()
            .url(urlWithKey)
            .build()
        chain.proceed(keyed)
    }

    // 3) Build the HTTP client
    private val httpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)             // retry if connection drops
        .addInterceptor(apiKeyInterceptor)          // inject key first
        .addInterceptor(loggingInterceptor)         // then log
        .connectTimeout(30, TimeUnit.SECONDS)       // time to establish TCP
        .readTimeout   (45, TimeUnit.SECONDS)       // time to read headers + body
        .writeTimeout  (45, TimeUnit.SECONDS)       // time to write request body
        .callTimeout   (60, TimeUnit.SECONDS)       // overall time for the call
        .build()

    // 4) Retrofit instance
    val spoonacularService: SpoonacularApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularApi::class.java)
    }
}
