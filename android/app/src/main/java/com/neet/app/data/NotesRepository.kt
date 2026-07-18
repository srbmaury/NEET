package com.neet.app.data

import com.neet.app.data.model.NotesResponse
import com.neet.app.data.model.Subject
import java.io.IOException

sealed interface NotesResult {
    data class Success(val notes: NotesResponse) : NotesResult
    data class Failure(val message: String) : NotesResult
}

class NotesRepository(private val apiService: NotesApiService) {

    suspend fun getNotes(subject: Subject, topic: String): NotesResult {
        return try {
            NotesResult.Success(apiService.getNotes(subject.name, topic))
        } catch (e: IOException) {
            NotesResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: retrofit2.HttpException) {
            NotesResult.Failure("Couldn't load notes for this topic. Please try again.")
        }
    }
}
