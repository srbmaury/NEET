package com.neet.app.ui.mocktest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.MockTestRepository
import com.neet.app.data.local.MOCK_TEST_STATUS_IN_PROGRESS
import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection
import com.neet.app.ui.components.MarkdownText
import com.neet.app.ui.question.ExplanationCard
import com.neet.app.ui.question.OptionRow
import com.neet.app.ui.question.QuestionUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    repository: MockTestRepository,
    testId: String,
    readOnly: Boolean,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MockTestViewModel = viewModel(
        factory = MockTestViewModelFactory(repository, testId, readOnly),
    )
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val remainingMillis = if (!readOnly) {
        rememberRemainingMillis(uiState.session?.startedAt, uiState.session?.durationMillis)
    } else {
        null
    }

    LaunchedEffect(remainingMillis != null && remainingMillis <= 0) {
        if (!readOnly && remainingMillis != null && remainingMillis <= 0 &&
            uiState.session?.status == MOCK_TEST_STATUS_IN_PROGRESS && !isSubmitting
        ) {
            isSubmitting = true
            viewModel.submit()
            onSubmitted(testId)
        }
    }

    val currentQuestions = viewModel.questionsFor(uiState.currentSubject, uiState.currentSection)
    val currentEntity = currentQuestions.getOrNull(uiState.currentIndex)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (readOnly) "Review" else "Mock Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (remainingMillis != null) {
                        Text(
                            formatRemaining(remainingMillis),
                            color = if (remainingMillis < 5 * 60_000L) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (currentQuestions.isEmpty() && uiState.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = Subject.entries.indexOf(uiState.currentSubject)) {
                Subject.entries.forEach { subject ->
                    Tab(
                        selected = subject == uiState.currentSubject,
                        onClick = { viewModel.selectSubject(subject) },
                        text = { Text(subject.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            TabRow(selectedTabIndex = if (uiState.currentSection == TestSection.A) 0 else 1) {
                Tab(
                    selected = uiState.currentSection == TestSection.A,
                    onClick = { viewModel.selectSection(TestSection.A) },
                    text = { Text("Section A") },
                )
                Tab(
                    selected = uiState.currentSection == TestSection.B,
                    onClick = { viewModel.selectSection(TestSection.B) },
                    text = { Text("Section B") },
                )
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            ) {
                if (currentEntity == null) {
                    Text("No questions in this section", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val question = viewModel.decodeQuestion(currentEntity)
                    Text(
                        "Q${uiState.currentIndex + 1} of ${currentQuestions.size} • ${question.topic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.padding(4.dp))
                    MarkdownText(
                        markdown = question.stem,
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                    Spacer(Modifier.padding(8.dp))

                    if (readOnly) {
                        val readyState = QuestionUiState.Ready(
                            question = question,
                            selectedOptionKey = currentEntity.selectedOptionKey,
                            isAnswered = true,
                        )
                        question.options.forEach { option ->
                            OptionRow(option = option, state = readyState, onClick = {})
                            Spacer(Modifier.padding(4.dp))
                        }
                        Spacer(Modifier.padding(8.dp))
                        ExplanationCard(question)
                    } else {
                        question.options.forEach { option ->
                            MockTestOptionRow(
                                option = option,
                                selected = option.key == currentEntity.selectedOptionKey,
                                onClick = { viewModel.selectAnswer(option.key) },
                            )
                            Spacer(Modifier.padding(4.dp))
                        }

                        Spacer(Modifier.padding(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearAnswer() },
                                modifier = Modifier.weight(0.8f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text("Clear", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { viewModel.toggleMarkedForReview() },
                                modifier = Modifier.weight(0.95f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(if (currentEntity.markedForReview) "Unmark" else "Mark", maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                modifier = Modifier.weight(1.35f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text("Save & Next", maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.padding(12.dp))
                MockTestPalette(
                    questions = currentQuestions,
                    currentIndex = uiState.currentIndex,
                    onJump = { index -> viewModel.jumpTo(index) },
                )

                if (!readOnly) {
                    Spacer(Modifier.padding(12.dp))
                    Button(
                        onClick = { showSubmitDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Submit Test")
                    }
                }
                Spacer(Modifier.padding(16.dp))
            }
        }
    }

    if (showSubmitDialog) {
        val attempted = uiState.questions.count { it.selectedOptionKey != null }
        val total = uiState.questions.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit mock test?") },
            text = { Text("Attempted $attempted of $total questions. Unattempted: ${total - attempted}.") },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitDialog = false
                    coroutineScope.launch {
                        isSubmitting = true
                        viewModel.submit()
                        onSubmitted(testId)
                    }
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun rememberRemainingMillis(startedAt: Long?, durationMillis: Long?): Long {
    var remaining by remember { mutableStateOf(Long.MAX_VALUE) }
    LaunchedEffect(startedAt, durationMillis) {
        if (startedAt == null || durationMillis == null) return@LaunchedEffect
        val deadline = startedAt + durationMillis
        while (true) {
            val now = System.currentTimeMillis()
            remaining = deadline - now
            if (remaining <= 0) break
            delay(1000)
        }
    }
    return remaining
}

private fun formatRemaining(millis: Long): String {
    val clamped = millis.coerceAtLeast(0)
    val totalSeconds = clamped / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
