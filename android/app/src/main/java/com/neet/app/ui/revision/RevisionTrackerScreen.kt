package com.neet.app.ui.revision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.neet.app.data.RevisionRepository
import com.neet.app.data.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionTrackerScreen(
    repository: RevisionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RevisionTrackerViewModel = viewModel(
        factory = RevisionTrackerViewModelFactory(repository),
    )
    val syllabusEntriesBySubject by viewModel.syllabusEntriesBySubject.collectAsState()
    val customEntries by viewModel.customEntries.collectAsState()
    var newTopicsText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Revision Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Text(
                    "Add custom topics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newTopicsText,
                        onValueChange = { newTopicsText = it },
                        label = { Text("e.g. a, b") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            viewModel.addCustomTopics(newTopicsText)
                            newTopicsText = ""
                        },
                    ) {
                        Text("Add")
                    }
                }
            }

            if (customEntries.isNotEmpty()) {
                item {
                    Text(
                        "Custom topics",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(customEntries, key = { "custom_${it.topic}" }) { entry ->
                    RevisionRow(
                        entry = entry,
                        onIncrement = { viewModel.incrementRevision(entry.topic, null) },
                    )
                }
            }

            Subject.entries.forEach { subject ->
                item(key = "header_${subject.name}") {
                    Text(
                        subject.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(syllabusEntriesBySubject[subject].orEmpty(), key = { "${it.subject}_${it.topic}" }) { entry ->
                    RevisionRow(
                        entry = entry,
                        onIncrement = { viewModel.incrementRevision(entry.topic, subject) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RevisionRow(entry: RevisionEntry, onIncrement: () -> Unit) {
    val revised = entry.revisionCount > 0
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (revised) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(entry.topic, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "Revised ${entry.revisionCount}×",
                style = MaterialTheme.typography.labelMedium,
                color = if (revised) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.padding(end = 8.dp),
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.Add, contentDescription = "Log a revision")
            }
        }
    }
}
