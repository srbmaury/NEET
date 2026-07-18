package com.neet.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog

@Composable
fun NotesTopicPickerScreen(
    onOpenNotes: (Subject, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubject by remember { mutableStateOf(Subject.BOTANY) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Concepts & formulas", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pick a topic for a quick reference sheet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            "Choose a subject",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Subject.entries.forEach { subject ->
                FilterChip(
                    selected = subject == selectedSubject,
                    onClick = { selectedSubject = subject },
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
                    selected = false,
                    onClick = { onOpenNotes(selectedSubject, topic) },
                    label = { Text(topic) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
