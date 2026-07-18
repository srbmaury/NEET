package com.neet.app.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neet.app.data.HistoryRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import com.neet.app.domain.suggestedDifficulty
import com.neet.app.ui.components.TopicRow

@Composable
fun TopicPickerScreen(
    historyRepository: HistoryRepository,
    onStartQuestion: (Subject, String, Difficulty) -> Unit,
    onOpenPracticeModes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubject by remember { mutableStateOf(Subject.PHYSICS) }
    var selectedTopic by remember { mutableStateOf(TopicCatalog.topicsFor(Subject.PHYSICS).first()) }
    val topicStats by historyRepository.topicStats().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose a subject", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Subject.entries.forEach { subject ->
                FilterChip(
                    selected = subject == selectedSubject,
                    onClick = {
                        selectedSubject = subject
                        selectedTopic = TopicCatalog.topicsFor(subject).first()
                    },
                    label = { Text(subject.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.height(40.dp),
                )
            }
        }

        Text("Choose a topic", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(TopicCatalog.topicsFor(selectedSubject)) { topic ->
                TopicRow(
                    topic = topic,
                    weightage = TopicCatalog.weightageOf(selectedSubject, topic),
                    selected = topic == selectedTopic,
                    onClick = { selectedTopic = topic },
                )
            }
        }

        Button(
            onClick = {
                // Difficulty is no longer picked by hand — it's re-rolled fresh on every tap from
                // a distribution weighted toward Medium/Hard (see suggestedDifficulty), so pressing
                // "Start practicing" again on the same topic can land a different difficulty.
                val stat = topicStats.firstOrNull { it.subject == selectedSubject.name && it.topic == selectedTopic }
                onStartQuestion(selectedSubject, selectedTopic, suggestedDifficulty(stat))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Start practicing")
        }

        Text(
            "More practice modes →",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clickable(onClick = onOpenPracticeModes),
        )
    }
}
