package com.neet.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neet.app.data.NotesRepository
import com.neet.app.data.NotesResult
import com.neet.app.data.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        _uiState.value = NotesUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getNotes(subject, topic)) {
                is NotesResult.Success ->
                    _uiState.value = NotesUiState.Ready(result.notes.contentMarkdown, result.notes.cached)
                is NotesResult.Failure -> _uiState.value = NotesUiState.Error(result.message)
            }
        }
    }
}
