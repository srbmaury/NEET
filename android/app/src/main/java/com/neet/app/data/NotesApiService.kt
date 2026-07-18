package com.neet.app.data

import com.neet.app.data.model.NotesResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface NotesApiService {
    @GET("notes/{subject}/{topic}")
    suspend fun getNotes(
        @Path("subject") subject: String,
        @Path("topic") topic: String,
    ): NotesResponse
}
