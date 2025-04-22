package com.unofficial.ahl.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network module for setting up API connection
 */
object NetworkModule {
    private const val BASE_URL = "https://kalanit.hebrew-academy.org.il/api/"
    
    /**
     * Creates and returns a configured Gson instance
     */
    private fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * Creates and returns a configured OkHttpClient instance
     */
    private fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Creates and returns a configured Retrofit instance
     */
    private fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Creates and returns the API service interface
     */
    fun provideAhlApi(): AhlApi {
        val gson = provideGson()
        val okHttpClient = provideOkHttpClient()
        val retrofit = provideRetrofit(gson, okHttpClient)
        return retrofit.create(AhlApi::class.java)
    }
} 