package com.neet.app.ui.flashcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.neet.app.data.model.NoteCard
import com.neet.app.data.model.Subject
import com.neet.app.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    repository: NotesRepository,
    subject: Subject,
    topic: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FlashcardsViewModel = viewModel(
        factory = FlashcardsViewModelFactory(repository, subject, topic),
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
                is FlashcardsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FlashcardsUiState.Error -> {
                    Text(
                        state.message,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is FlashcardsUiState.Ready -> {
                    val card = state.cards[state.currentIndex]
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Card ${state.currentIndex + 1} / ${state.cards.size} · ${card.type.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        FlashcardFace(
                            card = card,
                            revealed = state.revealed,
                            onClick = { if (!state.revealed) viewModel.reveal() },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.padding(top = 12.dp))
                        if (state.revealed) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.next() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Didn't know it")
                                }
                                Button(
                                    onClick = { viewModel.next() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Knew it")
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.reveal() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Reveal")
                            }
                        }
                    }
                }
                is FlashcardsUiState.Complete -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("You've reviewed every card", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "for $topic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardFace(
    card: NoteCard,
    revealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                MarkdownText(
                    markdown = card.term,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                if (revealed) {
                    Spacer(Modifier.height(16.dp))
                    MarkdownText(
                        markdown = card.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tap to reveal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
