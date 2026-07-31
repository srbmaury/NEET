package com.neet.app.ui.solver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neet.app.data.QuestionRepository
import com.neet.app.data.model.SolveQuestionImageRequest
import com.neet.app.data.model.SolvedQuestion
import com.neet.app.ui.components.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private sealed interface SolverState {
    data object Empty : SolverState
    data object Loading : SolverState
    data class Ready(val solution: SolvedQuestion) : SolverState
    data class Error(val message: String) : SolverState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageQuestionSolveScreen(
    repository: QuestionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var state by remember { mutableStateOf<SolverState>(SolverState.Empty) }
    var pendingCameraPhoto by remember { mutableStateOf<File?>(null) }

    fun solve(uri: Uri) {
        scope.launch {
            state = SolverState.Loading
            val request = withContext(Dispatchers.Default) {
                createImageRequest(context, uri)
            }
            if (request == null) {
                state = SolverState.Error("We couldn't read that image. Please try another photo.")
                return@launch
            }
            selectedImage = request.preview
            state = repository.solveQuestionImage(request.request).fold(
                onSuccess = { SolverState.Ready(it) },
                onFailure = { SolverState.Error("Couldn't solve the image. Check your connection and try again.") },
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) solve(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val photo = pendingCameraPhoto
        pendingCameraPhoto = null
        if (saved && photo != null) {
            solve(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo))
        } else {
            photo?.delete()
            state = SolverState.Error("No photo was captured. Please try again.")
        }
    }

    fun takePhoto() {
        val photo = File(context.cacheDir, "question_photos").apply { mkdirs() }
            .let { directory -> File.createTempFile("question_", ".jpg", directory) }
        pendingCameraPhoto = photo
        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Solve from photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Upload a clear photo of a question. AI will read it and show a worked solution.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = ::takePhoto, modifier = Modifier.fillMaxWidth()) {
                Text("Take question photo")
            }
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedImage == null) "Choose from gallery" else "Choose a different photo")
            }
            selectedImage?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected question photo",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (val current = state) {
                SolverState.Empty -> Unit
                SolverState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SolverState.Error -> {
                    Text(current.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Button(onClick = ::takePhoto, modifier = Modifier.fillMaxWidth()) { Text("Try taking another photo") }
                    OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose from gallery")
                    }
                }
                is SolverState.Ready -> SolutionContent(current.solution)
            }
        }
    }
}

@Composable
private fun SolutionContent(solution: SolvedQuestion) {
    Text("Question", style = MaterialTheme.typography.titleMedium)
    MarkdownText(solution.questionText, style = MaterialTheme.typography.bodyLarge)
    Text("Answer", style = MaterialTheme.typography.titleMedium)
    MarkdownText(solution.answer, style = MaterialTheme.typography.titleLarge)
    Text("Solution", style = MaterialTheme.typography.titleMedium)
    MarkdownText(solution.solution, style = MaterialTheme.typography.bodyLarge)
    Text("Key concept", style = MaterialTheme.typography.titleMedium)
    MarkdownText(solution.keyConcept, style = MaterialTheme.typography.bodyLarge)
    if (solution.confidenceNote.isNotBlank()) {
        Text(solution.confidenceNote, color = MaterialTheme.colorScheme.error)
    }
}

private data class PreparedImage(val request: SolveQuestionImageRequest, val preview: Bitmap)

private fun createImageRequest(context: Context, uri: Uri): PreparedImage? = runCatching {
    val source = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return null
    val largestSide = maxOf(source.width, source.height)
    val scale = if (largestSide > 1600) 1600f / largestSide else 1f
    val width = (source.width * scale).toInt().coerceAtLeast(1)
    val height = (source.height * scale).toInt().coerceAtLeast(1)
    val resized = if (scale < 1f) Bitmap.createScaledBitmap(source, width, height, true) else source
    val bytes = ByteArrayOutputStream().use { output ->
        check(resized.compress(Bitmap.CompressFormat.JPEG, 85, output))
        output.toByteArray()
    }
    check(bytes.size <= 5 * 1024 * 1024)
    PreparedImage(SolveQuestionImageRequest(Base64.encodeToString(bytes, Base64.NO_WRAP), "image/jpeg"), resized)
}.getOrNull()
