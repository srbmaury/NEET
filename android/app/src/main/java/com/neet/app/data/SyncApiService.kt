package com.neet.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SyncApiService {
    @POST("sync/push")
    suspend fun push(@Body payload: BackupPayload)

    @GET("sync/pull")
    suspend fun pull(): BackupPayload
}
