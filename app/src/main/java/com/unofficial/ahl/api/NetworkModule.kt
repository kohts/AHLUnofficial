package com.unofficial.ahl.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import okio.Buffer

/**
 * Network module for setting up API connection
 *
 * Sample request: https://kalanit.hebrew-academy.org.il/api/Ac/?SearchString=%D7%A9%D7%9B%D7%A0%D7%A2
 * Sample response:
 [
  {
    "keywordWithoutNikud": "שכנע_פועל",
    "menukadWithoutNikud": "שכנע",
    "ktiv_male": "שוכנע",
    "score": 0,
    "menukad": "שֻׁכְנַע",
    "keyword": "שֻׁכְנַע_פועל",
    "title": "שֻׁכְנַע (שוכנע), בניין פוּעל",
    "itemType": 1,
    "hagdara": "בא (או מוּבָא) לידי הַכָּרה בצִדקת טַענה ",
    "isRashi": 1,
    "kodBinyan": 0,
    "ItemTypeName": "AcItemPoal"
  },
  {
    "keywordWithoutNikud": "שכנע_פועל",
    "menukadWithoutNikud": "שכנע",
    "ktiv_male": "שכנע",
    "score": 0,
    "menukad": "שִׁכְנֵעַ",
    "keyword": "שִׁכְנֵעַ_פועל",
    "title": "שִׁכְנֵעַ (שכנע), בניין פיעל",
    "itemType": 1,
    "hagdara": "מֵביא לידי הכרה בצִדקת טַענה",
    "isRashi": 1,
    "kodBinyan": 0,
    "ItemTypeName": "AcItemPoal"
  },
  {
    "keywordWithoutNikud": "שכנע_פועל",
    "menukadWithoutNikud": null,
    "ktiv_male": null,
    "score": 0,
    "menukad": null,
    "keyword": "שִׁכְנֵעַ_פועל",
    "title": "שַׁכְנַע (שכנע) \u003E שִׁכְנֵעַ (שכנע), בניין פיעל",
    "itemType": 5,
    "hagdara": "מֵביא לידי הכרה בצִדקת טַענה",
    "isRashi": 2,
    "kodBinyan": 0,
    "ItemTypeName": "AcItemNote"
  }
]
 *
 */
object NetworkModule {
    private const val BASE_URL = "https://kalanit.hebrew-academy.org.il/api/"
    private const val HEBREW_ACADEMY_URL = "https://hebrew-academy.org.il/"
    
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
     * Creates and returns a configured Retrofit instance for the API
     */
    private fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Creates and returns a configured Retrofit instance for fetching HTML content
     */
    private fun provideHtmlRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(HEBREW_ACADEMY_URL)
            .client(okHttpClient)
            .build()
    }
    
    /**
     * Custom converter factory for handling raw HTML responses
     */
    private class RawResponseConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, String>? {
            return if (type == String::class.java) {
                Converter { body -> body.string() }
            } else {
                null
            }
        }
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