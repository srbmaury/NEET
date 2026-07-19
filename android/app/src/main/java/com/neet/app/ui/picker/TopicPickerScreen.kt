package com.neet.app.ui.picker

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    onStartSprint: (Subject, String) -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubject by remember { mutableStateOf(Subject.PHYSICS) }
    var selectedTopic by remember { mutableStateOf(TopicCatalog.topicsFor(Subject.PHYSICS).first()) }
    val topicStats by historyRepository.topicStats().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Practice", style = MaterialTheme.typography.headlineSmall)
            // Timed Sprint isn't here — it depends on whatever topic is currently selected below,
            // same as "Start practicing", so it sits next to that button instead. Everything else
            // that isn't topic-dependent (Smart Practice, Quick Reference, Coverage Heatmap,
            // Revision Tracker) lives one tap away on the "More" screen this opens.
            IconButton(onClick = onOpenMore) {
                Icon(Icons.Filled.Menu, contentDescription = "More")
            }
        }

        Text(
            "Choose a subject",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    // Difficulty is no longer picked by hand — it's re-rolled fresh on every tap
                    // from a distribution weighted toward Medium/Hard (see suggestedDifficulty),
                    // so pressing "Start practicing" again on the same topic can land a different
                    // difficulty.
                    val stat = topicStats.firstOrNull {
                        it.subject == selectedSubject.name && it.topic == selectedTopic
                    }
                    onStartQuestion(selectedSubject, selectedTopic, suggestedDifficulty(stat))
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Start practicing")
            }
            OutlinedButton(
                onClick = { onStartSprint(selectedSubject, selectedTopic) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Timed Sprint")
            }
        }
    }
}
