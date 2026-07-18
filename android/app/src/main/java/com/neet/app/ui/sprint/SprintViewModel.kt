package com.neet.app.ui.sprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val SPRINT_QUESTION_COUNT = 10
const val SPRINT_DURATION_SECONDS = 10 * 60

sealed interface SprintUiState {
    data object Loading : SprintUiState
    data class InProgress(
        val questionIndex: Int,
        val correctSoFar: Int,
        val secondsRemaining: Int,
    ) : SprintUiState
    data class Complete(val correct: Int, val answered: Int) : SprintUiState
}

/** No marks/negative-marking and nothing persisted to Room — a deliberately lighter-weight drill
 * than a mock test, ephemeral by design (see plan). Adaptive difficulty is re-rolled per question
 * from a single topicStats snapshot taken at session start, same as Smart Practice. */
class SprintViewModel(
    historyRepository: HistoryRepository,
    subject: Subject,
    topic: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SprintUiState>(SprintUiState.Loading)
    val uiState: StateFlow<SprintUiState> = _uiState.asStateFlow()

    var topicStat: TopicStat? = null
        private set

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val stats = historyRepository.topicStats().first()
            topicStat = stats.firstOrNull { it.subject == subject.name && it.topic == topic }
            _uiState.value = SprintUiState.InProgress(
                questionIndex = 0,
                correctSoFar = 0,
                secondsRemaining = SPRINT_DURATION_SECONDS,
            )
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val state = _uiState.value
                if (state !is SprintUiState.InProgress) break
                val remaining = state.secondsRemaining - 1
                if (remaining <= 0) {
                    _uiState.value = SprintUiState.Complete(state.correctSoFar, state.questionIndex)
                    break
                }
                _uiState.value = state.copy(secondsRemaining = remaining)
            }
        }
    }

    fun advance(lastAnswerCorrect: Boolean) {
        val state = _uiState.value
        if (state !is SprintUiState.InProgress) return

        val correctSoFar = state.correctSoFar + if (lastAnswerCorrect) 1 else 0
        val nextIndex = state.questionIndex + 1
        _uiState.value = if (nextIndex >= SPRINT_QUESTION_COUNT) {
            timerJob?.cancel()
            SprintUiState.Complete(correctSoFar, nextIndex)
        } else {
            state.copy(questionIndex = nextIndex, correctSoFar = correctSoFar)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class SprintViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SprintViewModel(historyRepository, subject, topic) as T
    }
}
