package com.neet.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.AnsweredQuestionEntity
import com.neet.app.data.local.SubjectStat
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import com.neet.app.domain.Weightage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val WEAK_ACCURACY_THRESHOLD = 60
private const val MAX_FOCUS_AREAS = 10

class StatsViewModel(historyRepository: HistoryRepository) : ViewModel() {

    private val _selectedSubject = MutableStateFlow(Subject.PHYSICS)
    val selectedSubject: StateFlow<Subject> = _selectedSubject.asStateFlow()

    val subjectStats: StateFlow<List<SubjectStat>> = historyRepository.subjectStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<AnsweredQuestionEntity>> = historyRepository.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Filtering by subject happens before ranking/capping to MAX_FOCUS_AREAS — otherwise a
    // subject's weak high-weightage topics could get crowded out of a global top-10 by other
    // subjects and never show up here even when selected.
    val focusAreas: StateFlow<List<FocusArea>> = combine(
        historyRepository.topicStats(),
        selectedSubject,
    ) { stats, subject -> computeFocusAreas(stats, subject) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
    }
}

private fun computeFocusAreas(stats: List<TopicStat>, subject: Subject): List<FocusArea> {
    val statsByKey = stats.associateBy { it.subject to it.topic }

    return TopicCatalog.topicsFor(subject).mapNotNull { topic ->
        val stat = statsByKey[subject.name to topic]
        val accuracyPercent = stat?.takeIf { it.total > 0 }?.let { it.correct * 100 / it.total }
        val status = when {
            stat == null || stat.total == 0 -> FocusStatus.NOT_ATTEMPTED
            accuracyPercent != null && accuracyPercent < WEAK_ACCURACY_THRESHOLD -> FocusStatus.WEAK
            else -> null
        } ?: return@mapNotNull null

        FocusArea(
            subject = subject,
            topic = topic,
            weightage = TopicCatalog.weightageOf(subject, topic),
            status = status,
            accuracyPercent = accuracyPercent,
        )
    }.sortedWith(
        compareByDescending<FocusArea> { it.weightage.rank() }
            .thenByDescending { it.status.rank() }
            .thenBy { it.accuracyPercent ?: -1 },
    ).take(MAX_FOCUS_AREAS)
}

private fun Weightage.rank(): Int = when (this) {
    Weightage.HIGH -> 3
    Weightage.MEDIUM -> 2
    Weightage.LOW -> 1
}

private fun FocusStatus.rank(): Int = when (this) {
    FocusStatus.NOT_ATTEMPTED -> 2
    FocusStatus.WEAK -> 1
}

class StatsViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StatsViewModel(historyRepository) as T
    }
}
