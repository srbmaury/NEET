package com.neet.app.ui.more

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.NotesRepository
import com.neet.app.domain.TopicCatalog
import com.neet.app.pdf.buildAllNotesMarkdown
import com.neet.app.pdf.renderMarkdownToPdf
import com.neet.app.pdf.writePdfToUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NotesExportState {
    data object Idle : NotesExportState
    data class Loading(val completed: Int, val total: Int) : NotesExportState
    data object Success : NotesExportState
    data class Error(val message: String) : NotesExportState
}

class NotesExportViewModel(private val notesRepository: NotesRepository) : ViewModel() {

    private val _state = MutableStateFlow<NotesExportState>(NotesExportState.Idle)
    val state: StateFlow<NotesExportState> = _state.asStateFlow()

    fun export(context: Context, uri: Uri) {
        if (_state.value is NotesExportState.Loading) return
        viewModelScope.launch {
            _state.value = NotesExportState.Loading(0, TopicCatalog.allTopics().size)
            try {
                val markdown = buildAllNotesMarkdown(notesRepository) { completed, total ->
                    _state.value = NotesExportState.Loading(completed, total)
                }
                // Off the main thread — this does real CPU work (StaticLayout over the whole
                // combined document, plus a LaTeX bitmap render per formula across all 84 topics),
                // easily enough to trip an ANR if run on Dispatchers.Main.immediate (the
                // viewModelScope default).
                val document = withContext(Dispatchers.Default) {
                    renderMarkdownToPdf(context, markdown)
                }
                withContext(Dispatchers.IO) {
                    writePdfToUri(context, document, uri)
                }
                _state.value = NotesExportState.Success
            } catch (e: Exception) {
                _state.value = NotesExportState.Error(e.message ?: "Something went wrong while generating the PDF")
            }
        }
    }

    fun dismiss() {
        _state.value = NotesExportState.Idle
    }
}

class NotesExportViewModelFactory(
    private val notesRepository: NotesRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NotesExportViewModel(notesRepository) as T
    }
}
