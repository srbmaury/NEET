package com.neet.app.ui.smartpractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Subject
import com.neet.app.domain.buildSmartPracticeQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SmartPracticeUiState {
    data object Loading : SmartPracticeUiState
    data object Empty : SmartPracticeUiState
    data class InProgress(
        val queue: List<Pair<Subject, String>>,
        val currentIndex: Int,
        val correctSoFar: Int,
    ) : SmartPracticeUiState
    data class Complete(val correct: Int, val total: Int) : SmartPracticeUiState
}

/** The queue is built once, from a single snapshot of topicStats when the session starts — it
 * deliberately does not re-rank mid-session as answers come in, so the 10-item list stays stable
 * for the whole session instead of shifting under the student's feet. */
class SmartPracticeViewModel(private val historyRepository: HistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SmartPracticeUiState>(SmartPracticeUiState.Loading)
    val uiState: StateFlow<SmartPracticeUiState> = _uiState.asStateFlow()

    // Snapshot taken once alongside the queue itself — reused so each question's adaptive
    // difficulty (see suggestedDifficulty) reflects accuracy as of session start, consistent with
    // the queue not re-ranking mid-session either.
    var topicStatsSnapshot: List<TopicStat> = emptyList()
        private set

    init {
        viewModelScope.launch {
            val stats = historyRepository.topicStats().first()
            topicStatsSnapshot = stats
            val queue = buildSmartPracticeQueue(stats)
            _uiState.value = if (queue.isEmpty()) {
                SmartPracticeUiState.Empty
            } else {
                SmartPracticeUiState.InProgress(queue, currentIndex = 0, correctSoFar = 0)
            }
        }
    }

    fun advance(lastAnswerCorrect: Boolean) {
        val state = _uiState.value
        if (state !is SmartPracticeUiState.InProgress) return

        val correctSoFar = state.correctSoFar + if (lastAnswerCorrect) 1 else 0
        val nextIndex = state.currentIndex + 1
        _uiState.value = if (nextIndex >= state.queue.size) {
            SmartPracticeUiState.Complete(correct = correctSoFar, total = state.queue.size)
        } else {
            state.copy(currentIndex = nextIndex, correctSoFar = correctSoFar)
        }
    }
}

class SmartPracticeViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SmartPracticeViewModel(historyRepository) as T
    }
}
