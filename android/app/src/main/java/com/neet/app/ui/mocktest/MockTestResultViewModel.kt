package com.neet.app.ui.mocktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.MockTestRepository
import com.neet.app.data.local.MockTestSessionEntity
import com.neet.app.domain.SubjectScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class MockTestResultUiState(
    val session: MockTestSessionEntity? = null,
    val subjectScores: List<SubjectScore> = emptyList(),
)

class MockTestResultViewModel(
    private val repository: MockTestRepository,
    private val testId: String,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(MockTestResultUiState())
    val uiState: StateFlow<MockTestResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(testId)
            val subjectScores = session?.perSubjectScoreJson
                ?.let { runCatching { json.decodeFromString<List<SubjectScore>>(it) }.getOrNull() }
                .orEmpty()
            _uiState.value = MockTestResultUiState(session = session, subjectScores = subjectScores)
        }
    }
}

class MockTestResultViewModelFactory(
    private val repository: MockTestRepository,
    private val testId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MockTestResultViewModel(repository, testId) as T
    }
}
