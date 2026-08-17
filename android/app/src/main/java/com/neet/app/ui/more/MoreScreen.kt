package com.neet.app.ui.more

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neet.app.data.NotesRepository
import com.neet.app.BuildConfig
import com.neet.app.pdf.openPdf

private data class MoreFeature(val title: String, val description: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onOpenSmartPractice: () -> Unit,
    onOpenQuickReference: () -> Unit,
    onOpenSyllabusHeatmap: () -> Unit,
    onOpenRevisionTracker: () -> Unit,
    notesRepository: NotesRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exportViewModel: NotesExportViewModel = viewModel(
        factory = NotesExportViewModelFactory(notesRepository),
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
            is NotesExportState.Success -> {
                openPdf(context, state.uri)
                exportViewModel.dismiss()
            }
            is NotesExportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                exportViewModel.dismiss()
            }
            else -> Unit
        }
    }

    val features = listOf(
        MoreFeature("Smart Practice", "Auto-mixed weak topics across all subjects", onOpenSmartPractice),
        MoreFeature("Quick Reference", "Constants, trig, vectors, calculus", onOpenQuickReference),
        MoreFeature("Coverage Heatmap", "See accuracy across every topic in the syllabus", onOpenSyllabusHeatmap),
        MoreFeature("Revision Tracker", "Track how many times you've revised each topic", onOpenRevisionTracker),
        MoreFeature(
            "Download All Notes (PDF)",
            "One PDF: quick reference plus every topic's notes",
            onClick = { createDocumentLauncher.launch("NEET_Notes.pdf") },
        ),
        MoreFeature(
            "Privacy & data policy",
            "How Neet handles your account, progress, and question photos",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.BASE_URL}privacy")))
            },
        ),
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

    val loadingState = exportState as? NotesExportState.Loading
    if (loadingState != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Generating PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Fetching notes: ${loadingState.completed}/${loadingState.total}")
                }
            },
        )
    }
}
