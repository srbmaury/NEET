package com.neet.app.ui.mocktest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.MockTestRepository
import com.neet.app.pdf.buildMockTestMarkdown
import com.neet.app.pdf.renderMarkdownToPdf
import com.neet.app.pdf.writePdfToUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MockTestExportState {
    data object Idle : MockTestExportState
    data object Generating : MockTestExportState
    data class Success(val uri: Uri) : MockTestExportState
    data class Error(val message: String) : MockTestExportState
}

class MockTestExportViewModel(
    private val repository: MockTestRepository,
    private val testId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<MockTestExportState>(MockTestExportState.Idle)
    val state: StateFlow<MockTestExportState> = _state.asStateFlow()

    fun export(context: Context, uri: Uri) {
        if (_state.value is MockTestExportState.Generating) return
        viewModelScope.launch {
            _state.value = MockTestExportState.Generating
            try {
                val session = repository.getSession(testId) ?: error("Test not found")
                val questions = repository.getQuestions(testId)
                val markdown = buildMockTestMarkdown(session, questions, repository::decodeQuestion)
                // Off the main thread, same reasoning as NotesExportViewModel — StaticLayout over
                // a full test's worth of questions plus every embedded LaTeX formula is real CPU
                // work, easily enough to trip an ANR on Dispatchers.Main.immediate.
                val document = withContext(Dispatchers.Default) {
                    renderMarkdownToPdf(context, markdown)
                }
                withContext(Dispatchers.IO) {
                    writePdfToUri(context, document, uri)
                }
                _state.value = MockTestExportState.Success(uri)
            } catch (e: Exception) {
                _state.value = MockTestExportState.Error(e.message ?: "Something went wrong while generating the PDF")
            }
        }
    }

    fun dismiss() {
        _state.value = MockTestExportState.Idle
    }
}

class MockTestExportViewModelFactory(
    private val repository: MockTestRepository,
    private val testId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MockTestExportViewModel(repository, testId) as T
    }
}
