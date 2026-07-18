package com.neet.app.ui.mocktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.MockTestGenerationResult
import com.neet.app.data.MockTestRepository
import com.neet.app.data.local.MockTestSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GenerationUiState {
    data object Idle : GenerationUiState
    data class Generating(val elapsedSeconds: Int) : GenerationUiState
    data class Failed(val message: String) : GenerationUiState
}

class MockTestHomeViewModel(private val repository: MockTestRepository) : ViewModel() {

    // A Flow (not a one-shot fetch) so this reflects the current session status even when it
    // changes from a different screen instance — e.g. submitting from the taking screen must be
    // reflected here immediately when the user returns to this tab, not just at ViewModel init.
    val activeSession: StateFlow<MockTestSessionEntity?> = repository.activeSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    val completedTests: StateFlow<List<MockTestSessionEntity>> = repository.completedTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.reconcileExpiredSession()
        }
    }

    fun startNewTest(onStarted: (String) -> Unit) {
        if (_generationState.value is GenerationUiState.Generating) return
        viewModelScope.launch {
            var elapsed = 0
            _generationState.value = GenerationUiState.Generating(0)
            val tickerJob = launch {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    elapsed += 1
                    _generationState.value = GenerationUiState.Generating(elapsed)
                }
            }
            when (val result = repository.startNewTest()) {
                is MockTestGenerationResult.Success -> {
                    tickerJob.cancel()
                    _generationState.value = GenerationUiState.Idle
                    onStarted(result.testId)
                }
                is MockTestGenerationResult.Failure -> {
                    tickerJob.cancel()
                    _generationState.value = GenerationUiState.Failed(result.message)
                }
            }
        }
    }

    fun dismissError() {
        _generationState.value = GenerationUiState.Idle
    }
}

class MockTestHomeViewModelFactory(
    private val repository: MockTestRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MockTestHomeViewModel(repository) as T
    }
}
