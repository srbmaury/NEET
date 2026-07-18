package com.neet.app.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.NotesRepository
import com.neet.app.data.model.Subject
import com.neet.app.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    repository: NotesRepository,
    subject: Subject,
    topic: String,
    onBack: () -> Unit,
    onOpenFlashcards: (Subject, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(repository, subject, topic),
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(topic) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            when (val state = uiState) {
                is NotesUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.padding(4.dp))
                        Text(
                            "Generating notes for the first time — this can take a moment",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                is NotesUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, textAlign = TextAlign.Center)
                        Spacer(Modifier.padding(8.dp))
                        Button(onClick = { viewModel.loadNotes() }) {
                            Text("Retry")
                        }
                    }
                }
                is NotesUiState.Ready -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            subject.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        if (state.cards.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { onOpenFlashcards(subject, topic) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Text("Practice as flashcards (${state.cards.size})")
                            }
                        }
                        Spacer(Modifier.padding(4.dp))
                        MarkdownText(
                            markdown = state.contentMarkdown,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        }
    }
}
