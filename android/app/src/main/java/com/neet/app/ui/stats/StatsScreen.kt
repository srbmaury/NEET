package com.neet.app.ui.stats

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.local.AnsweredQuestionEntity
import com.neet.app.data.local.SubjectStat
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.domain.Weightage
import com.neet.app.ui.components.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun dateLabelFor(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(dateFormatter)
    }
}

private fun answeredDate(record: AnsweredQuestionEntity): LocalDate =
    Instant.ofEpochMilli(record.answeredAt).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun StatsScreen(
    historyRepository: HistoryRepository,
    onOpenQuestion: (String) -> Unit,
    onPracticeTopic: (Subject, String, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(historyRepository))
    val stats by viewModel.subjectStats.collectAsState()
    val history by viewModel.history.collectAsState()
    val focusAreas by viewModel.focusAreas.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val backupJson = historyRepository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(backupJson.toByteArray())
                    }
                }
            }.onSuccess {
                Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Backup failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val backupJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not read the selected file")
                }
                historyRepository.importBackupJson(backupJson)
            }.onSuccess { result ->
                val message = "Restored ${result.answerCount} answers, ${result.mockTestCount} mock tests"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val totalAnswered = stats.sumOf { it.total }
    val totalCorrect = stats.sumOf { it.correct }
    val statsBySubject = stats.associateBy { it.subject }
    val filteredHistory = history.filter { it.subject == selectedSubject.name }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Your progress", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { backupLauncher.launch("neet_backup.json") }) {
                    Text("Backup")
                }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json")) }) {
                    Text("Restore")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Overall", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (totalAnswered == 0) {
                        "No questions answered yet"
                    } else {
                        "$totalCorrect / $totalAnswered correct (${percent(totalCorrect, totalAnswered)}%)"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (focusAreas.isNotEmpty()) {
            Text(
                "Focus areas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(focusAreas, key = { "${it.subject}_${it.topic}" }) { focusArea ->
                    FocusAreaCard(
                        focusArea = focusArea,
                        onClick = {
                            onPracticeTopic(focusArea.subject, focusArea.topic, Difficulty.MEDIUM)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Subject.entries.forEach { subject ->
                val subjectStat = statsBySubject[subject.name]
                val label = subject.name.lowercase().replaceFirstChar { it.uppercase() } +
                    if (subjectStat != null) " ${subjectStat.correct}/${subjectStat.total}" else ""
                FilterChip(
                    selected = subject == selectedSubject,
                    onClick = { viewModel.selectSubject(subject) },
                    label = { Text(label) },
                )
            }
        }

        Text(
            "Recent answers",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    "No questions answered in " +
                        selectedSubject.name.lowercase().replaceFirstChar { it.uppercase() } + " yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            val groupedByDate = filteredHistory.groupBy { answeredDate(it) }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groupedByDate.forEach { (date, entriesForDate) ->
                    item(key = "date_$date") {
                        Text(
                            dateLabelFor(date),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(entriesForDate, key = { it.id }) { record ->
                        HistoryCard(record, onClick = { onOpenQuestion(record.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusAreaCard(focusArea: FocusArea, onClick: () -> Unit) {
    val weightageColor = when (focusArea.weightage) {
        Weightage.HIGH -> Color(0xFFC62828)
        Weightage.MEDIUM -> Color(0xFFEF6C00)
        Weightage.LOW -> Color(0xFF616161)
    }
    Card(
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${focusArea.weightage.name} weightage",
                style = MaterialTheme.typography.labelSmall,
                color = weightageColor,
            )
            Text(
                focusArea.topic,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Text(
                focusArea.subject.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                if (focusArea.status == FocusStatus.NOT_ATTEMPTED) {
                    "Not attempted yet"
                } else {
                    "${focusArea.accuracyPercent}% accuracy"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun HistoryCard(record: AnsweredQuestionEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                record.topic,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            MarkdownText(
                markdown = record.stem,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            val correctColor = Color(0xFF2E7D32)
            val wrongColor = Color(0xFFC62828)
            MarkdownText(
                markdown = "Your answer: ${record.selectedOptionKey}. ${record.selectedOptionText}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (record.isCorrect) correctColor else wrongColor,
                ),
            )
            if (!record.isCorrect) {
                MarkdownText(
                    markdown = "Correct answer: ${record.correctOptionKey}. ${record.correctOptionText}",
                    style = MaterialTheme.typography.bodySmall.copy(color = correctColor),
                )
            }
        }
    }
}

private fun percent(correct: Int, total: Int): Int =
    if (total == 0) 0 else (correct * 100) / total
