package com.neet.app.ui.sprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.QuestionResult
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject
import com.neet.app.domain.suggestedDifficulty
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
        val difficulty: Difficulty,
        // Non-null once the background prefetch for this index has landed — see prefetchNext().
        val preloadedQuestion: Question? = null,
    ) : SprintUiState
    data class Complete(val correct: Int, val answered: Int) : SprintUiState
}

/** No marks/negative-marking and nothing persisted to Room — a deliberately lighter-weight drill
 * than a mock test, ephemeral by design (see plan). Adaptive difficulty is re-rolled per question
 * from a single topicStats snapshot taken at session start, same as Smart Practice. */
class SprintViewModel(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SprintUiState>(SprintUiState.Loading)
    val uiState: StateFlow<SprintUiState> = _uiState.asStateFlow()

    private var topicStat: TopicStat? = null

    private var timerJob: Job? = null

    // Keyed by question index. Populated by prefetchNext() while the previous question is on
    // screen, so by the time the student advances, the next question is usually already sitting
    // here instead of making them wait on a fresh generation call.
    private val prefetchBuffer = mutableMapOf<Int, Pair<Difficulty, Question>>()

    init {
        viewModelScope.launch {
            val stats = historyRepository.topicStats().first()
            topicStat = stats.firstOrNull { it.subject == subject.name && it.topic == topic }
            // The 10-minute clock is meant to time answering, not the first question's generation
            // call — so unlike every later index (which can fall back to an on-demand fetch inside
            // QuestionScreen if its prefetch hasn't landed yet), index 0 is awaited here before the
            // timer starts at all.
            val first = fetchQuestion()
            _uiState.value = SprintUiState.InProgress(
                questionIndex = 0,
                correctSoFar = 0,
                secondsRemaining = SPRINT_DURATION_SECONDS,
                difficulty = first?.first ?: suggestedDifficulty(topicStat),
                preloadedQuestion = first?.second,
            )
            startTimer()
            prefetchNext(1)
        }
    }

    private fun enterIndex(index: Int, correctSoFar: Int, secondsRemaining: Int): SprintUiState.InProgress {
        val buffered = prefetchBuffer.remove(index)
        val difficulty = buffered?.first ?: suggestedDifficulty(topicStat)
        val state = SprintUiState.InProgress(index, correctSoFar, secondsRemaining, difficulty, buffered?.second)
        prefetchNext(index + 1)
        return state
    }

    private suspend fun fetchQuestion(): Pair<Difficulty, Question>? {
        val difficulty = suggestedDifficulty(topicStat)
        val excludeStems = historyRepository.recentStems(subject.name, topic)
        val request = GenerateQuestionRequest(subject, topic, difficulty, excludeStems)
        return when (val result = repository.generateQuestion(request)) {
            is QuestionResult.Success -> difficulty to result.question
            is QuestionResult.Failure -> null // that question falls back to an on-demand fetch instead
        }
    }

    private fun prefetchNext(index: Int) {
        if (index >= SPRINT_QUESTION_COUNT || prefetchBuffer.containsKey(index)) return
        viewModelScope.launch {
            fetchQuestion()?.let { prefetchBuffer[index] = it }
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
            enterIndex(nextIndex, correctSoFar, state.secondsRemaining)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class SprintViewModelFactory(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SprintViewModel(repository, historyRepository, subject, topic) as T
    }
}
