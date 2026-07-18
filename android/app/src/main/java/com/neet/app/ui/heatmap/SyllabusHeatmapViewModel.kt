package com.neet.app.ui.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HeatmapEntry(
    val subject: Subject,
    val topic: String,
    val accuracyPercent: Int?,
    val total: Int,
)

class SyllabusHeatmapViewModel(historyRepository: HistoryRepository) : ViewModel() {

    val entriesBySubject: StateFlow<Map<Subject, List<HeatmapEntry>>> = historyRepository.topicStats()
        .map(::computeHeatmap)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

private fun computeHeatmap(stats: List<TopicStat>): Map<Subject, List<HeatmapEntry>> {
    val statsByKey = stats.associateBy { it.subject to it.topic }
    return Subject.entries.associateWith { subject ->
        TopicCatalog.topicsFor(subject).map { topic ->
            val stat = statsByKey[subject.name to topic]
            val accuracy = stat?.takeIf { it.total > 0 }?.let { it.correct * 100 / it.total }
            HeatmapEntry(subject, topic, accuracy, stat?.total ?: 0)
        }
    }
}

class SyllabusHeatmapViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SyllabusHeatmapViewModel(historyRepository) as T
    }
}
