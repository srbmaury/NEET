package com.neet.app.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.RevisionRepository
import com.neet.app.data.local.RevisionEntity
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RevisionEntry(val topic: String, val subject: Subject?, val revisionCount: Int)

class RevisionTrackerViewModel(private val repository: RevisionRepository) : ViewModel() {

    private val revisions: StateFlow<List<RevisionEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Every syllabus topic shows up here even with zero revisions (count defaults to 0) — the
    // point is to see the whole syllabus at a glance, not just topics you've already touched.
    val syllabusEntriesBySubject: StateFlow<Map<Subject, List<RevisionEntry>>> = revisions
        .map { entities ->
            val countByTopic = entities.associate { it.topic to it.revisionCount }
            Subject.entries.associateWith { subject ->
                TopicCatalog.topicsFor(subject).map { topic ->
                    RevisionEntry(topic, subject, countByTopic[topic] ?: 0)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Custom topics only ever appear here once explicitly added — unlike syllabus topics, there's
    // no fixed catalog to fall back to for the zero-count case.
    val customEntries: StateFlow<List<RevisionEntry>> = revisions
        .map { entities ->
            entities.filter { it.subject == null }.map { RevisionEntry(it.topic, null, it.revisionCount) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun incrementRevision(topic: String, subject: Subject?) {
        viewModelScope.launch { repository.incrementRevision(topic, subject?.name) }
    }

    fun addCustomTopics(commaSeparated: String) {
        viewModelScope.launch { repository.addCustomTopics(commaSeparated) }
    }
}

class RevisionTrackerViewModelFactory(
    private val repository: RevisionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RevisionTrackerViewModel(repository) as T
    }
}
