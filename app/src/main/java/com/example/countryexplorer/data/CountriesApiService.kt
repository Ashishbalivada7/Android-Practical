package com.example.countryexplorer.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException

interface CountriesApiService {
    @GET("countries.json")
    suspend fun getAllCountries(): List<CountryResponse>

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/mledoze/countries/master/dist/"

        fun create(): CountriesApiService {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} ${response.message}")
                    }
                    response
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CountriesApiService::class.java)
        }
    }
}
