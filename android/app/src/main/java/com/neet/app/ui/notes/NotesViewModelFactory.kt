package com.neet.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neet.app.data.NotesRepository
import com.neet.app.data.model.Subject

class NotesViewModelFactory(
    private val repository: NotesRepository,
    private val subject: Subject,
    private val topic: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NotesViewModel(repository, subject, topic) as T
    }
}
