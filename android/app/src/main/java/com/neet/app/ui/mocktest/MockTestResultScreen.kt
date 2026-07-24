package com.neet.app.ui.mocktest

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.MockTestRepository
import com.neet.app.domain.SubjectScore
import com.neet.app.pdf.openPdf

private val correctColor = Color(0xFF2E7D32)
private val wrongColor = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestResultScreen(
    repository: MockTestRepository,
    testId: String,
    onBack: () -> Unit,
    onReview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MockTestResultViewModel = viewModel(
        factory = MockTestResultViewModelFactory(repository, testId),
    )
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val exportViewModel: MockTestExportViewModel = viewModel(
        factory = MockTestExportViewModelFactory(repository, testId),
    )
    val exportState by exportViewModel.state.collectAsState()
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            exportViewModel.export(context, uri)
        }
    }
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is MockTestExportState.Success -> {
                openPdf(context, state.uri)
                exportViewModel.dismiss()
            }
            is MockTestExportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                exportViewModel.dismiss()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val session = uiState.session
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Overall Score", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${session.totalScore ?: 0} / ${session.maxPossibleMarks ?: 720}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    ScoreBreakdownText(
                        correct = session.correctCount ?: 0,
                        wrong = session.wrongCount ?: 0,
                        unanswered = session.unansweredCount ?: 0,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.padding(top = 16.dp))
            Text("By Subject", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(top = 4.dp))

            uiState.subjectScores.forEach { subjectScore ->
                SubjectScoreCard(subjectScore)
                Spacer(Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.padding(top = 8.dp))
            Button(
                onClick = { onReview(testId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Review Answers")
            }
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    createDocumentLauncher.launch("NEET_MockTest_${testId.take(8)}.pdf")
                },
                enabled = exportState !is MockTestExportState.Generating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (exportState is MockTestExportState.Generating) {
                        "Generating PDF…"
                    } else {
                        "Download PDF (questions + answers)"
                    },
                )
            }
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun SubjectScoreCard(subjectScore: SubjectScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                subjectScore.subject.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${subjectScore.marks} / ${subjectScore.maxMarks} marks",
                style = MaterialTheme.typography.bodyMedium,
            )
            ScoreBreakdownText(
                correct = subjectScore.correctCount,
                wrong = subjectScore.wrongCount,
                unanswered = subjectScore.unansweredCount,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Color-coded to match the correct/wrong convention used elsewhere (e.g. history cards) —
 * a single uniform color here would make "Wrong" read as good news. */
@Composable
private fun ScoreBreakdownText(correct: Int, wrong: Int, unanswered: Int, style: androidx.compose.ui.text.TextStyle) {
    val neutralColor = MaterialTheme.colorScheme.secondary
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = correctColor)) { append("Correct: $correct") }
            withStyle(SpanStyle(color = neutralColor)) { append(" • ") }
            withStyle(SpanStyle(color = wrongColor)) { append("Wrong: $wrong") }
            withStyle(SpanStyle(color = neutralColor)) { append(" • Unanswered: $unanswered") }
        },
        style = style,
    )
}

