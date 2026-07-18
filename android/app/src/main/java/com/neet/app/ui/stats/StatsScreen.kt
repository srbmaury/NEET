package com.neet.app.ui.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.AuthRepository
import com.neet.app.data.HistoryRepository
import com.neet.app.data.SyncRepository
import com.neet.app.data.local.AnsweredQuestionEntity
import com.neet.app.data.local.MistakeTopicStat
import com.neet.app.data.local.SubjectStat
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.domain.Weightage
import com.neet.app.ui.components.AnswerRecordCard
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    historyRepository: HistoryRepository,
    authRepository: AuthRepository,
    syncRepository: SyncRepository,
    onOpenQuestion: (String) -> Unit,
    onPracticeTopic: (Subject, String, Difficulty) -> Unit,
    onReviewMistakes: (Subject, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onOpenSyllabusHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(historyRepository))
    val stats by viewModel.subjectStats.collectAsState()
    val history by viewModel.history.collectAsState()
    val focusAreas by viewModel.focusAreas.collectAsState()
    val mistakeTopics by viewModel.mistakeTopics.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = false)
    val accountEmail by authRepository.accountEmail.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Cloud sync (via the account below) replaced manual backup/restore — quietly keep this
    // device's data current with the server each time this screen is opened while signed in,
    // rather than requiring an explicit "Sync Now" tap.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) syncRepository.syncNow()
    }

    val totalAnswered = stats.sumOf { it.total }
    val totalCorrect = stats.sumOf { it.correct }
    val statsBySubject = stats.associateBy { it.subject }
    val filteredHistory = history.filter { it.subject == selectedSubject.name }

    // Single scrollable list for the whole screen (rather than a fixed header above a separately
    // scrolling history list) — the subject tabs are pinned via stickyHeader so switching subjects
    // stays reachable without scrolling back up, once the account/overall/focus-areas content
    // above it has scrolled out of view.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text("Your progress", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Text(
                "Syllabus →",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onOpenSyllabusHub)
                    .padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            AccountSection(
                isLoggedIn = isLoggedIn,
                accountEmail = accountEmail,
                onSignIn = onNavigateToLogin,
                onSignUp = onNavigateToSignup,
                onLogOut = {
                    coroutineScope.launch {
                        // Push before dropping the token, so the latest local state (anything since
                        // the last silent sync) is saved server-side before it gets harder to reach.
                        syncRepository.push()
                        authRepository.logout()
                    }
                },
            )
        }

        item {
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
        }

        if (focusAreas.isNotEmpty()) {
            item {
                Text(
                    "Focus areas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            item {
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
        }

        if (mistakeTopics.isNotEmpty()) {
            item {
                Text(
                    "Mistakes to review",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(mistakeTopics, key = { "${it.subject}_${it.topic}" }) { mistake ->
                        MistakeTopicCard(
                            mistake = mistake,
                            onClick = {
                                onReviewMistakes(Subject.valueOf(mistake.subject), mistake.topic)
                            },
                        )
                    }
                }
            }
        }

        stickyHeader(key = "subject_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Subject.entries.forEach { subject ->
                    val subjectStat = statsBySubject[subject.name]
                    val label = subject.name.lowercase().replaceFirstChar { it.uppercase() } +
                        if (subjectStat != null) " ${subjectStat.correct}/${subjectStat.total}" else ""
                    FilterChip(
                        selected = subject == selectedSubject,
                        onClick = { viewModel.selectSubject(subject) },
                        label = { Text(label) },
                        modifier = Modifier.height(40.dp),
                    )
                }
            }
        }

        item {
            Text(
                "Recent answers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        if (filteredHistory.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        "No questions answered in " +
                            selectedSubject.name.lowercase().replaceFirstChar { it.uppercase() } + " yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        } else {
            val groupedByDate = filteredHistory.groupBy { answeredDate(it) }
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
                    AnswerRecordCard(
                        record = record,
                        onClick = { onOpenQuestion(record.id) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    isLoggedIn: Boolean,
    accountEmail: String?,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onLogOut: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (isLoggedIn) {
                Text("Signed in", style = MaterialTheme.typography.labelMedium)
                Text(
                    accountEmail ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedButton(onClick = onLogOut) { Text("Log Out") }
            } else {
                Text("Sync across devices", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Optional — practice works fully without an account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSignUp) { Text("Sign Up") }
                    OutlinedButton(onClick = onSignIn) { Text("Log In") }
                }
            }
        }
    }
}

@Composable
private fun MistakeTopicCard(mistake: MistakeTopicStat, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                mistake.topic,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Text(
                mistake.subject.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "${mistake.wrongCount} wrong",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFC62828),
            )
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

private fun percent(correct: Int, total: Int): Int =
    if (total == 0) 0 else (correct * 100) / total
