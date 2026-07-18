package com.neet.app.data

import com.neet.app.data.model.GenerateMockTestRequest
import com.neet.app.data.model.MockTestResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface MockTestApiService {
    @POST("mock-test/generate")
    suspend fun generateMockTest(@Body request: GenerateMockTestRequest): MockTestResponse
}
