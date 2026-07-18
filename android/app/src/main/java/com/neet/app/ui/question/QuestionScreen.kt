package com.neet.app.ui.question

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Question
import com.neet.app.data.model.QuestionOption
import com.neet.app.data.model.Subject
import com.neet.app.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(
    repository: QuestionRepository,
    historyRepository: HistoryRepository,
    subject: Subject,
    topic: String,
    difficulty: Difficulty,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuestionViewModel = viewModel(
        factory = QuestionViewModelFactory(repository, historyRepository, subject, topic, difficulty),
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
                is QuestionUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is QuestionUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, textAlign = TextAlign.Center)
                        Spacer(Modifier.padding(8.dp))
                        Button(onClick = { viewModel.loadQuestion() }) {
                            Text("Retry")
                        }
                    }
                }
                is QuestionUiState.Ready -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "$topic • $difficulty",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.padding(4.dp))
                        MarkdownText(
                            markdown = state.question.stem,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Spacer(Modifier.padding(8.dp))

                        state.question.options.forEach { option ->
                            OptionRow(
                                option = option,
                                state = state,
                                onClick = { viewModel.selectOption(option.key) },
                            )
                            Spacer(Modifier.padding(4.dp))
                        }

                        if (state.isAnswered) {
                            Spacer(Modifier.padding(8.dp))
                            ExplanationCard(state.question)
                            Spacer(Modifier.padding(12.dp))
                            Button(
                                onClick = { viewModel.loadQuestion() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Next question")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionRow(
    option: QuestionOption,
    state: QuestionUiState.Ready,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        !state.isAnswered -> MaterialTheme.colorScheme.surfaceVariant
        option.key == state.question.correctOptionKey -> Color(0xFFC8E6C9)
        option.key == state.selectedOptionKey -> Color(0xFFFFCDD2)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !state.isAnswered, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        MarkdownText(
            markdown = "${option.key}. ${option.text}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}

@Composable
fun ExplanationCard(question: Question) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Why this is correct",
                style = MaterialTheme.typography.titleSmall,
                color = onSurfaceVariant,
            )
            MarkdownText(
                markdown = question.explanation.whyCorrect,
                style = MaterialTheme.typography.bodyMedium.copy(color = onSurfaceVariant),
            )
            Text(
                "Why the others are wrong",
                style = MaterialTheme.typography.titleSmall,
                color = onSurfaceVariant,
            )
            MarkdownText(
                markdown = question.explanation.whyOthersWrong,
                style = MaterialTheme.typography.bodyMedium.copy(color = onSurfaceVariant),
            )
            Text(
                "Key concept",
                style = MaterialTheme.typography.titleSmall,
                color = onSurfaceVariant,
            )
            MarkdownText(
                markdown = question.explanation.keyConcept,
                style = MaterialTheme.typography.bodyMedium.copy(color = onSurfaceVariant),
            )
        }
    }
}
