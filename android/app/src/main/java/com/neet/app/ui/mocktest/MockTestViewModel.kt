package com.neet.app.ui.mocktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.MockTestRepository
import com.neet.app.data.local.MockTestQuestionEntity
import com.neet.app.data.local.MockTestSessionEntity
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection
import com.neet.app.domain.MockTestScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MockTestUiState(
    val session: MockTestSessionEntity? = null,
    val questions: List<MockTestQuestionEntity> = emptyList(),
    val currentSubject: Subject = Subject.PHYSICS,
    val currentSection: TestSection = TestSection.A,
    val currentIndex: Int = 0,
)

class MockTestViewModel(
    private val repository: MockTestRepository,
    private val testId: String,
    val readOnly: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockTestUiState())
    val uiState: StateFlow<MockTestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(session = repository.getSession(testId))
        }
        repository.questionsForTest(testId).onEach { questions ->
            _uiState.value = _uiState.value.copy(questions = questions)
            if (!readOnly) markCurrentVisited()
        }.launchIn(viewModelScope)
    }

    fun decodeQuestion(entity: MockTestQuestionEntity): Question = repository.decodeQuestion(entity)

    fun questionsFor(subject: Subject, section: TestSection): List<MockTestQuestionEntity> =
        _uiState.value.questions
            .filter { it.subject == subject.name && it.section == section.name }
            .sortedBy { it.sectionIndex }

    fun currentQuestion(): MockTestQuestionEntity? {
        val state = _uiState.value
        return questionsFor(state.currentSubject, state.currentSection).getOrNull(state.currentIndex)
    }

    fun selectSubject(subject: Subject) {
        _uiState.value = _uiState.value.copy(currentSubject = subject, currentSection = TestSection.A, currentIndex = 0)
        if (!readOnly) markCurrentVisited()
    }

    fun selectSection(section: TestSection) {
        _uiState.value = _uiState.value.copy(currentSection = section, currentIndex = 0)
        if (!readOnly) markCurrentVisited()
    }

    fun jumpTo(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        if (!readOnly) markCurrentVisited()
    }

    fun nextQuestion() {
        val state = _uiState.value
        val list = questionsFor(state.currentSubject, state.currentSection)
        val nextIndex = state.currentIndex + 1
        if (nextIndex < list.size) {
            _uiState.value = state.copy(currentIndex = nextIndex)
            if (!readOnly) markCurrentVisited()
        }
    }

    private fun markCurrentVisited() {
        val entity = currentQuestion() ?: return
        if (entity.visited) return
        viewModelScope.launch {
            repository.markVisited(testId, entity.subject, entity.section, entity.sectionIndex)
        }
    }

    fun selectAnswer(optionKey: String) {
        if (readOnly) return
        val entity = currentQuestion() ?: return
        viewModelScope.launch {
            repository.selectAnswer(testId, entity.subject, entity.section, entity.sectionIndex, optionKey)
        }
    }

    fun clearAnswer() {
        if (readOnly) return
        val entity = currentQuestion() ?: return
        viewModelScope.launch {
            repository.clearAnswer(testId, entity.subject, entity.section, entity.sectionIndex)
        }
    }

    fun toggleMarkedForReview() {
        if (readOnly) return
        val entity = currentQuestion() ?: return
        viewModelScope.launch {
            repository.toggleMarkedForReview(
                testId,
                entity.subject,
                entity.section,
                entity.sectionIndex,
                !entity.markedForReview,
            )
        }
    }

    suspend fun submit(): MockTestScore = repository.submitTest(testId)
}

class MockTestViewModelFactory(
    private val repository: MockTestRepository,
    private val testId: String,
    private val readOnly: Boolean,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MockTestViewModel(repository, testId, readOnly) as T
    }
}
