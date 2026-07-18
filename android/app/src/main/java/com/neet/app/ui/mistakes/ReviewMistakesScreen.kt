package com.neet.app.ui.mistakes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.ui.components.AnswerRecordCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewMistakesScreen(
    historyRepository: HistoryRepository,
    subject: Subject,
    topic: String,
    onBack: () -> Unit,
    onOpenQuestion: (String) -> Unit,
    onPracticeTopic: (Subject, String, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReviewMistakesViewModel = viewModel(
        factory = ReviewMistakesViewModelFactory(historyRepository, subject.name, topic),
    )
    val wrongAnswers by viewModel.wrongAnswers.collectAsState()

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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Button(
                onClick = { onPracticeTopic(subject, topic, Difficulty.MEDIUM) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Practice this topic")
            }

            Text(
                if (wrongAnswers.isEmpty()) {
                    ""
                } else {
                    "${wrongAnswers.size} mistake${if (wrongAnswers.size == 1) "" else "s"} to review"
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wrongAnswers, key = { it.id }) { record ->
                    // Topic is already the screen's title, so repeating it on every card would
                    // just be noise here — unlike the Progress tab's cross-topic history list.
                    AnswerRecordCard(
                        record = record,
                        onClick = { onOpenQuestion(record.id) },
                        showTopic = false,
                    )
                }
            }
        }
    }
}
