package com.neet.app.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.AnsweredQuestionRecord
import com.neet.app.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestionReviewViewModel(
    private val historyRepository: HistoryRepository,
    private val answerId: String,
) : ViewModel() {

    private val _record = MutableStateFlow<AnsweredQuestionRecord?>(null)
    val record: StateFlow<AnsweredQuestionRecord?> = _record.asStateFlow()

    init {
        viewModelScope.launch {
            _record.value = historyRepository.getAnsweredQuestion(answerId)
        }
    }
}

class QuestionReviewViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val answerId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return QuestionReviewViewModel(historyRepository, answerId) as T
    }
}
