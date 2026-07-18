package com.neet.app.data

import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Question
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("questions/generate")
    suspend fun generateQuestion(@Body request: GenerateQuestionRequest): Question
}
