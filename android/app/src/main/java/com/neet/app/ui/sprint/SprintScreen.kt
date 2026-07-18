package com.neet.app.ui.sprint

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.model.Subject
import com.neet.app.ui.question.QuestionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SprintScreen(
    repository: QuestionRepository,
    historyRepository: HistoryRepository,
    subject: Subject,
    topic: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SprintViewModel = viewModel(
        factory = SprintViewModelFactory(repository, historyRepository, subject, topic),
    )
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is SprintUiState.Loading -> {
            Scaffold(modifier = modifier) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        is SprintUiState.InProgress -> {
            key(state.questionIndex) {
                QuestionScreen(
                    repository = repository,
                    historyRepository = historyRepository,
                    subject = subject,
                    topic = topic,
                    difficulty = state.difficulty,
                    onBack = onBack,
                    onNext = { wasCorrect -> viewModel.advance(wasCorrect) },
                    viewModelKey = "sprint_${state.questionIndex}",
                    preloadedQuestion = state.preloadedQuestion,
                    topBarActions = {
                        TimerChip(secondsRemaining = state.secondsRemaining, questionIndex = state.questionIndex)
                    },
                    modifier = modifier,
                )
            }
        }
        is SprintUiState.Complete -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { Text("Timed Sprint") },
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
                    Text("Sprint complete", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.correct} / ${state.answered} correct",
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

@Composable
private fun TimerChip(secondsRemaining: Int, questionIndex: Int) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Text(
            "Q${questionIndex + 1}/10 · %d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
