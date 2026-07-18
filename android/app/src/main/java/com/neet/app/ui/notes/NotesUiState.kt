package com.neet.app.ui.notes

sealed interface NotesUiState {
    data object Loading : NotesUiState
    data class Error(val message: String) : NotesUiState
    data class Ready(val contentMarkdown: String, val cached: Boolean) : NotesUiState
}
