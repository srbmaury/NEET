package com.neet.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.AnsweredQuestionEntity
import com.neet.app.data.local.MistakeTopicStat
import com.neet.app.data.local.SubjectStat
import com.neet.app.data.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(historyRepository: HistoryRepository) : ViewModel() {

    private val _selectedSubject = MutableStateFlow(Subject.PHYSICS)
    val selectedSubject: StateFlow<Subject> = _selectedSubject.asStateFlow()

    val subjectStats: StateFlow<List<SubjectStat>> = historyRepository.subjectStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<AnsweredQuestionEntity>> = historyRepository.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mistakeTopics: StateFlow<List<MistakeTopicStat>> = historyRepository.mistakeTopicStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
    }
}

class StatsViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StatsViewModel(historyRepository) as T
    }
}
