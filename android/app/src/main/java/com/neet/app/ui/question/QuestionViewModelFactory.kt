package com.neet.app.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject

class QuestionViewModelFactory(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
    private val subject: Subject,
    private val topic: String,
    private val difficulty: Difficulty,
    private val preloadedQuestion: Question? = null,
    private val standalone: Boolean = true,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return QuestionViewModel(
            repository,
            historyRepository,
            subject,
            topic,
            difficulty,
            preloadedQuestion,
            standalone,
        ) as T
    }
}
