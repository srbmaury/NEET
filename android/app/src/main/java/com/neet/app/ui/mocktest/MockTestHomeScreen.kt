package com.neet.app.ui.mocktest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.MockTestRepository
import com.neet.app.data.local.MockTestSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

@Composable
fun MockTestHomeScreen(
    repository: MockTestRepository,
    onOpenTest: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MockTestHomeViewModel = viewModel(factory = MockTestHomeViewModelFactory(repository))
    val activeSession by viewModel.activeSession.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val completedTests by viewModel.completedTests.collectAsState()
    var showStartDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Mock Test", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.padding(top = 8.dp))

        when (val state = generationState) {
            is GenerationUiState.Generating -> {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Generating your mock test…", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.padding(top = 8.dp))
                        LinearProgressIndicator(
                            progress = { (state.elapsedSeconds / 240f).coerceAtMost(0.95f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.padding(top = 4.dp))
                        Text(
                            "${state.elapsedSeconds}s elapsed — usually takes 3-5 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            is GenerationUiState.Failed -> {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Couldn't generate a mock test", style = MaterialTheme.typography.titleMedium)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.padding(top = 8.dp))
                        Button(onClick = { viewModel.startNewTest(onOpenTest) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            GenerationUiState.Idle -> {
                if (activeSession != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("You have a test in progress", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.padding(top = 8.dp))
                            Button(
                                onClick = { onOpenTest(activeSession!!.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Resume Test")
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start New Mock Test")
                    }
                }
            }
        }

        Spacer(Modifier.padding(top = 16.dp))
        Text("Past attempts", style = MaterialTheme.typography.titleMedium)

        if (completedTests.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    "No mock tests submitted yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(completedTests, key = { it.id }) { session ->
                    CompletedTestCard(session, onClick = { onOpenResult(session.id) })
                }
            }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start a full mock test?") },
            text = {
                Text(
                    "This generates the full NEET format — 200 questions across all 4 " +
                        "subjects — and usually takes 3-5 minutes. You'll have 3h20m to " +
                        "complete it once it starts.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStartDialog = false
                    viewModel.startNewTest(onOpenTest)
                }) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CompletedTestCard(session: MockTestSessionEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Score: ${session.totalScore ?: 0} / ${session.maxPossibleMarks ?: 720}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                session.submittedAt?.let { dateFormatter.format(Date(it)) } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

