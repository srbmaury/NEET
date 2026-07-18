package com.neet.app.ui.heatmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.HistoryRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusHeatmapScreen(
    historyRepository: HistoryRepository,
    onBack: () -> Unit,
    onPracticeTopic: (Subject, String, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SyllabusHeatmapViewModel = viewModel(
        factory = SyllabusHeatmapViewModelFactory(historyRepository),
    )
    val entriesBySubject by viewModel.entriesBySubject.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Full syllabus") },
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
            Subject.entries.forEach { subject ->
                item(key = "header_${subject.name}") {
                    Text(
                        subject.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(entriesBySubject[subject].orEmpty(), key = { "${it.subject}_${it.topic}" }) { entry ->
                    HeatmapRow(
                        entry = entry,
                        onClick = { onPracticeTopic(entry.subject, entry.topic, Difficulty.MEDIUM) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapRow(entry: HeatmapEntry, onClick: () -> Unit) {
    val accuracy = entry.accuracyPercent
    val (badgeColor, badgeText) = when {
        accuracy == null -> Color(0xFF616161) to "Not attempted"
        accuracy < 50 -> Color(0xFFC62828) to "$accuracy%"
        accuracy < 75 -> Color(0xFFEF6C00) to "$accuracy%"
        else -> Color(0xFF2E7D32) to "$accuracy%"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.topic,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(badgeText, style = MaterialTheme.typography.labelMedium, color = badgeColor)
    }
}
