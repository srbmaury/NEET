package com.neet.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class NotesResponse(
    val subject: String,
    val topic: String,
    val contentMarkdown: String,
    val cached: Boolean,
)
