package com.neet.app.ui.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.AnsweredQuestionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReviewMistakesViewModel(
    historyRepository: HistoryRepository,
    subject: String,
    topic: String,
) : ViewModel() {

    val wrongAnswers: StateFlow<List<AnsweredQuestionEntity>> =
        historyRepository.wrongAnswersForTopic(subject, topic)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class ReviewMistakesViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val subject: String,
    private val topic: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReviewMistakesViewModel(historyRepository, subject, topic) as T
    }
}
