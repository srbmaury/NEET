package com.neet.app.ui.notes

import com.neet.app.data.model.NoteCard

sealed interface NotesUiState {
    data object Loading : NotesUiState
    data class Error(val message: String) : NotesUiState
    data class Ready(val contentMarkdown: String, val cards: List<NoteCard>, val cached: Boolean) : NotesUiState
}
