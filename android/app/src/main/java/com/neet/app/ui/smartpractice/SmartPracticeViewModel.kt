package com.neet.app.ui.smartpractice

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
import com.neet.app.domain.buildSmartPracticeQueue
import com.neet.app.domain.suggestedDifficulty
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
        val difficulty: Difficulty,
        // Non-null once the background prefetch for this index has landed — see prefetchNext().
        val preloadedQuestion: Question? = null,
    ) : SmartPracticeUiState
    data class Complete(val correct: Int, val total: Int) : SmartPracticeUiState
}

/** The queue is built once, from a single snapshot of topicStats when the session starts — it
 * deliberately does not re-rank mid-session as answers come in, so the 10-item list stays stable
 * for the whole session instead of shifting under the student's feet. */
class SmartPracticeViewModel(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmartPracticeUiState>(SmartPracticeUiState.Loading)
    val uiState: StateFlow<SmartPracticeUiState> = _uiState.asStateFlow()

    // Snapshot taken once alongside the queue itself — reused so each question's adaptive
    // difficulty (see suggestedDifficulty) reflects accuracy as of session start, consistent with
    // the queue not re-ranking mid-session either.
    private var topicStatsSnapshot: List<TopicStat> = emptyList()

    // Keyed by queue index. Populated by prefetchNext() while the previous index is on screen, so
    // by the time the student advances, the next question is usually already sitting here instead
    // of making them wait on a fresh generation call.
    private val prefetchBuffer = mutableMapOf<Int, Pair<Difficulty, Question>>()

    init {
        viewModelScope.launch {
            val stats = historyRepository.topicStats().first()
            topicStatsSnapshot = stats
            val queue = buildSmartPracticeQueue(stats)
            _uiState.value = if (queue.isEmpty()) {
                SmartPracticeUiState.Empty
            } else {
                enterIndex(queue, index = 0, correctSoFar = 0)
            }
        }
    }

    private fun enterIndex(
        queue: List<Pair<Subject, String>>,
        index: Int,
        correctSoFar: Int,
    ): SmartPracticeUiState.InProgress {
        val buffered = prefetchBuffer.remove(index)
        val difficulty = buffered?.first ?: suggestedDifficulty(statFor(queue[index]))
        val state = SmartPracticeUiState.InProgress(queue, index, correctSoFar, difficulty, buffered?.second)
        prefetchNext(queue, index + 1)
        return state
    }

    private fun statFor(entry: Pair<Subject, String>): TopicStat? {
        val (subject, topic) = entry
        return topicStatsSnapshot.firstOrNull { it.subject == subject.name && it.topic == topic }
    }

    private fun prefetchNext(queue: List<Pair<Subject, String>>, index: Int) {
        if (index >= queue.size || prefetchBuffer.containsKey(index)) return
        viewModelScope.launch {
            val (subject, topic) = queue[index]
            val difficulty = suggestedDifficulty(statFor(queue[index]))
            val excludeStems = historyRepository.recentStems(subject.name, topic)
            val request = GenerateQuestionRequest(subject, topic, difficulty, excludeStems)
            when (val result = repository.generateQuestion(request)) {
                is QuestionResult.Success -> prefetchBuffer[index] = difficulty to result.question
                is QuestionResult.Failure -> Unit // silently drop; that question loads on-demand instead
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
            enterIndex(state.queue, nextIndex, correctSoFar)
        }
    }
}

class SmartPracticeViewModelFactory(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SmartPracticeViewModel(repository, historyRepository) as T
    }
}
