package com.neet.app.ui.practicemodes

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import com.neet.app.ui.components.TopicRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeModesScreen(
    onBack: () -> Unit,
    onStartSmartPractice: () -> Unit,
    onStartSprint: (Subject, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubject by remember { mutableStateOf(Subject.PHYSICS) }
    var selectedTopic by remember { mutableStateOf(TopicCatalog.topicsFor(Subject.PHYSICS).first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More practice modes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Card(
                onClick = onStartSmartPractice,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Smart Practice", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Auto-mixed weak topics across all subjects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Timed Sprint", style = MaterialTheme.typography.titleMedium)
            Text(
                "10 questions, 10 minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                onClick = { onStartSprint(selectedSubject, selectedTopic) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Start Sprint")
            }
        }
    }
}
