package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "http://172.20.10.14:3000/"
    private const val LIGHT_CONTROL_URL = "http://172.20.10.13:5000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    val lightApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LIGHT_CONTROL_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}