package com.neet.app.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.QuestionResult
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestionViewModel(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
    private val subject: Subject,
    private val topic: String,
    private val difficulty: Difficulty,
    preloadedQuestion: Question? = null,
    // Smart Practice/Sprint own their own cross-question prefetching (they know what topic/
    // difficulty comes next; this ViewModel doesn't). Only plain Practice mode — where "Next
    // question" calls loadQuestion() on this same instance instead of a session onNext callback —
    // needs this ViewModel to prefetch its own follow-up question.
    private val standalone: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestionUiState>(
        preloadedQuestion?.let { QuestionUiState.Ready(it) } ?: QuestionUiState.Loading,
    )
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    private var prefetchedNext: Question? = null
    private var prefetchJob: Job? = null

    init {
        if (preloadedQuestion != null) {
            if (standalone) prefetchNext(preloadedQuestion.stem)
        } else {
            loadQuestion()
        }
    }

    fun loadQuestion() {
        val buffered = prefetchedNext
        if (buffered != null) {
            prefetchedNext = null
            _uiState.value = QuestionUiState.Ready(buffered)
            if (standalone) prefetchNext(buffered.stem)
            return
        }
        _uiState.value = QuestionUiState.Loading
        viewModelScope.launch {
            when (val result = fetchQuestion(emptyList())) {
                is QuestionResult.Success -> {
                    _uiState.value = QuestionUiState.Ready(result.question)
                    if (standalone) prefetchNext(result.question.stem)
                }
                is QuestionResult.Failure -> _uiState.value = QuestionUiState.Error(result.message)
            }
        }
    }

    // Runs right after the current question is shown, not when "Next" is tapped — so the next
    // question is usually already sitting in [prefetchedNext] by the time it's needed. The
    // just-shown stem is passed explicitly since historyRepository only knows about *answered*
    // questions, and the current one hasn't been answered yet at this point.
    private fun prefetchNext(currentStem: String) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            when (val result = fetchQuestion(listOf(currentStem))) {
                is QuestionResult.Success -> prefetchedNext = result.question
                is QuestionResult.Failure -> Unit // silently drop; loadQuestion() falls back to on-demand
            }
        }
    }

    private suspend fun fetchQuestion(extraExcludeStems: List<String>): QuestionResult {
        val excludeStems = historyRepository.recentStems(subject.name, topic) + extraExcludeStems
        val request = GenerateQuestionRequest(subject, topic, difficulty, excludeStems)
        return repository.generateQuestion(request)
    }

    fun selectOption(optionKey: String) {
        val current = _uiState.value
        if (current is QuestionUiState.Ready && !current.isAnswered) {
            _uiState.value = current.copy(selectedOptionKey = optionKey, isAnswered = true)
            viewModelScope.launch {
                historyRepository.recordAnswer(current.question, optionKey)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
    }
}
