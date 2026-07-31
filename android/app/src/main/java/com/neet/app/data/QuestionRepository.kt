package com.neet.app.data

import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Question
import com.neet.app.data.model.SolveQuestionImageRequest
import com.neet.app.data.model.SolvedQuestion
import java.io.IOException

sealed interface QuestionResult {
    data class Success(val question: Question) : QuestionResult
    data class Failure(val message: String) : QuestionResult
}

class QuestionRepository(private val apiService: ApiService) {

    suspend fun generateQuestion(request: GenerateQuestionRequest): QuestionResult {
        return try {
            QuestionResult.Success(apiService.generateQuestion(request))
        } catch (e: IOException) {
            QuestionResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: retrofit2.HttpException) {
            QuestionResult.Failure("The server had trouble generating a question. Please try again.")
        }
    }

    suspend fun solveQuestionImage(request: SolveQuestionImageRequest): Result<SolvedQuestion> = runCatching {
        apiService.solveQuestionImage(request)
    }
}
