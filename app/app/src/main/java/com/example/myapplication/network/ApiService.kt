package com.example.myapplication.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Multipart
import okhttp3.MultipartBody

data class LoginRequest(val name: String, val pwd: String)
data class LoginResponse(val success: Boolean)
data class RegisterRequest(val name: String, val pwd: String)
data class RegisterResponse(
    val success: Boolean,
    val message: String? = null,
    val username: String? = null,
    val password: String? = null)
data class ImagePathResponse(
    val success: Boolean,
    val username: String)
data class ControlRequest(val status: Boolean)
data class ControlResponse(val success: Boolean, val status: Boolean)

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    @POST("register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>
    @Multipart
    @POST("uploadImagePath")
    fun uploadImagePath(@Part file: MultipartBody.Part): Call<ImagePathResponse>
    //以下兩個用在控制電燈
    @POST("/control/light_1")
    fun controlLight_1(@Body request: ControlRequest): Call<ControlResponse>
    @POST("/control/light_2")
    fun controlLight_2(@Body request: ControlRequest): Call<ControlResponse>
    @POST("/control/fan")
    fun controlFan(@Body request: ControlRequest): Call<ControlResponse>
    @POST("/control/AirConditioner")
    fun controlAirConditioner(@Body request: ControlRequest): Call<ControlResponse>

}

