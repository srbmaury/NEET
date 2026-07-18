package com.neet.app.data

import retrofit2.http.GET

interface HealthApiService {
    @GET("health")
    suspend fun ping()
}
