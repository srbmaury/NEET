package com.neet.app.data

import com.neet.app.data.model.AuthResponse
import com.neet.app.data.model.LoginRequest
import com.neet.app.data.model.SignupRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @DELETE("auth/account")
    suspend fun deleteAccount()
}
