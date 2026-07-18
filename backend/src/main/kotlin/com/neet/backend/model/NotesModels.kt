package com.neet.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class NoteCard(val term: String, val content: String, val type: String)

@Serializable
data class NotesResponse(
    val subject: String,
    val topic: String,
    val contentMarkdown: String,
    val cards: List<NoteCard>,
    val cached: Boolean,
)
