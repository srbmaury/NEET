package com.neet.app.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.QuestionResult
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.GenerateQuestionRequest
import com.neet.app.data.model.Subject
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestionUiState>(QuestionUiState.Loading)
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    init {
        loadQuestion()
    }

    fun loadQuestion() {
        _uiState.value = QuestionUiState.Loading
        viewModelScope.launch {
            val excludeStems = historyRepository.recentStems(subject.name, topic)
            val request = GenerateQuestionRequest(subject, topic, difficulty, excludeStems)
            when (val result = repository.generateQuestion(request)) {
                is QuestionResult.Success -> _uiState.value = QuestionUiState.Ready(result.question)
                is QuestionResult.Failure -> _uiState.value = QuestionUiState.Error(result.message)
            }
        }
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
}
