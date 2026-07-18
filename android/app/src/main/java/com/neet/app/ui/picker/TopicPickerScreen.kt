package com.neet.app.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog

@Composable
fun TopicPickerScreen(
    onStartQuestion: (Subject, String, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubject by remember { mutableStateOf(Subject.BOTANY) }
    var selectedTopic by remember { mutableStateOf(TopicCatalog.topicsFor(Subject.BOTANY).first()) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose a subject", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Subject.entries.forEach { subject ->
                FilterChip(
                    selected = subject == selectedSubject,
                    onClick = {
                        selectedSubject = subject
                        selectedTopic = TopicCatalog.topicsFor(subject).first()
                    },
                    label = { Text(subject.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Text("Choose a topic", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(TopicCatalog.topicsFor(selectedSubject)) { topic ->
                FilterChip(
                    selected = topic == selectedTopic,
                    onClick = { selectedTopic = topic },
                    label = { Text(topic) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text("Difficulty", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Difficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = difficulty == selectedDifficulty,
                    onClick = { selectedDifficulty = difficulty },
                    label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Button(
            onClick = { onStartQuestion(selectedSubject, selectedTopic, selectedDifficulty) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Start practicing")
        }
    }
}
