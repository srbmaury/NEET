package com.neet.app.ui.smartpractice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.domain.suggestedDifficulty
import com.neet.app.ui.question.QuestionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPracticeScreen(
    repository: QuestionRepository,
    historyRepository: HistoryRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SmartPracticeViewModel = viewModel(
        factory = SmartPracticeViewModelFactory(historyRepository),
    )
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is SmartPracticeUiState.Loading -> {
            Scaffold(modifier = modifier) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        is SmartPracticeUiState.Empty -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { Text("Smart Practice") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                    Text(
                        "You're solid across every topic right now — nothing weak or unattempted " +
                            "to queue up. Nice work.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
        is SmartPracticeUiState.InProgress -> {
            val (subject, topic) = state.queue[state.currentIndex]
            val stat = viewModel.topicStatsSnapshot.firstOrNull {
                it.subject == subject.name && it.topic == topic
            }
            val difficulty = remember(state.currentIndex) { suggestedDifficulty(stat) }
            key(state.currentIndex) {
                QuestionScreen(
                    repository = repository,
                    historyRepository = historyRepository,
                    subject = subject,
                    topic = topic,
                    difficulty = difficulty,
                    onBack = onBack,
                    onNext = { wasCorrect -> viewModel.advance(wasCorrect) },
                    viewModelKey = "smart_practice_${state.currentIndex}",
                    modifier = modifier,
                )
            }
        }
        is SmartPracticeUiState.Complete -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { Text("Smart Practice") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.padding(top = 48.dp))
                    Text("Session complete", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.correct} / ${state.total} correct",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
