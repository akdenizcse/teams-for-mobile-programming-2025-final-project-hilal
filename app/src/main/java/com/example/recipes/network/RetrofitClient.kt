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

object RetrofitClient {
    private const val BASE_URL = "https://api.spoonacular.com/"

    // Logging interceptor (make sure you have the okhttp3-logging dependency)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor that injects your API key into every request
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        val originalUrl: HttpUrl = originalRequest.url

        // Append apiKey query param
        val urlWithKey = originalUrl.newBuilder()
            .addQueryParameter("apiKey", BuildConfig.SPOONACULAR_API_KEY)
            .build()

        val keyedRequest = originalRequest.newBuilder()
            .url(urlWithKey)
            .build()

        chain.proceed(keyedRequest)
    }

    // Single OkHttpClient with both interceptors
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit service instance
    val spoonacularService: SpoonacularApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularApi::class.java)
    }
}
