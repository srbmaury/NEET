package com.neet.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class MoreFeature(val title: String, val description: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onOpenSmartPractice: () -> Unit,
    onOpenQuickReference: () -> Unit,
    onOpenSyllabusHeatmap: () -> Unit,
    onOpenRevisionTracker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val features = listOf(
        MoreFeature("Smart Practice", "Auto-mixed weak topics across all subjects", onOpenSmartPractice),
        MoreFeature("Quick Reference", "Constants, trig, vectors, calculus", onOpenQuickReference),
        MoreFeature("Coverage Heatmap", "See accuracy across every topic in the syllabus", onOpenSyllabusHeatmap),
        MoreFeature("Revision Tracker", "Track how many times you've revised each topic", onOpenRevisionTracker),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            features.forEach { feature ->
                Card(
                    onClick = feature.onClick,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(feature.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            feature.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
