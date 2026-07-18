package com.neet.app.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.NotesRepository
import com.neet.app.data.NotesResult
import com.neet.app.data.model.NoteCard
import com.neet.app.data.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FlashcardsUiState {
    data object Loading : FlashcardsUiState
    data class Error(val message: String) : FlashcardsUiState
    data class Ready(val cards: List<NoteCard>, val currentIndex: Int, val revealed: Boolean) : FlashcardsUiState
    data object Complete : FlashcardsUiState
}

/** Re-fetches notes independently rather than being handed the already-fetched cards from
 * NoteScreen — the server-side cache (see NotesRoutes) makes a second request to the same
 * topic effectively free, and keeps this screen a standalone destination reachable on its own. */
class FlashcardsViewModel(
    private val repository: NotesRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardsUiState>(FlashcardsUiState.Loading)
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getNotes(subject, topic)) {
                is NotesResult.Success -> {
                    _uiState.value = if (result.notes.cards.isEmpty()) {
                        FlashcardsUiState.Error("No flashcards available for this topic.")
                    } else {
                        FlashcardsUiState.Ready(result.notes.cards, currentIndex = 0, revealed = false)
                    }
                }
                is NotesResult.Failure -> _uiState.value = FlashcardsUiState.Error(result.message)
            }
        }
    }

    fun reveal() {
        val state = _uiState.value
        if (state is FlashcardsUiState.Ready) {
            _uiState.value = state.copy(revealed = true)
        }
    }

    fun next() {
        val state = _uiState.value
        if (state !is FlashcardsUiState.Ready) return
        val nextIndex = state.currentIndex + 1
        _uiState.value = if (nextIndex >= state.cards.size) {
            FlashcardsUiState.Complete
        } else {
            state.copy(currentIndex = nextIndex, revealed = false)
        }
    }
}

class FlashcardsViewModelFactory(
    private val repository: NotesRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FlashcardsViewModel(repository, subject, topic) as T
    }
}
