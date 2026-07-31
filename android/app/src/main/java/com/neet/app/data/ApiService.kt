package com.neet.app.data

import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Question
import com.neet.app.data.model.SolveQuestionImageRequest
import com.neet.app.data.model.SolvedQuestion
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("questions/generate")
    suspend fun generateQuestion(@Body request: GenerateQuestionRequest): Question

    @POST("questions/solve-image")
    suspend fun solveQuestionImage(@Body request: SolveQuestionImageRequest): SolvedQuestion
}
