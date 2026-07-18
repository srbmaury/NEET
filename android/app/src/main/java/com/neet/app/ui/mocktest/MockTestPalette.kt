package com.neet.app.ui.mocktest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neet.app.data.local.MockTestQuestionEntity

enum class PaletteState { NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED_FOR_REVIEW, ANSWERED_AND_MARKED }

fun paletteStateFor(entity: MockTestQuestionEntity): PaletteState = when {
    entity.markedForReview && entity.selectedOptionKey != null -> PaletteState.ANSWERED_AND_MARKED
    entity.markedForReview -> PaletteState.MARKED_FOR_REVIEW
    entity.selectedOptionKey != null -> PaletteState.ANSWERED
    entity.visited -> PaletteState.NOT_ANSWERED
    else -> PaletteState.NOT_VISITED
}

private fun PaletteState.color(): Color = when (this) {
    PaletteState.NOT_VISITED -> Color(0xFFBDBDBD)
    PaletteState.NOT_ANSWERED -> Color(0xFFEF5350)
    PaletteState.ANSWERED -> Color(0xFF66BB6A)
    PaletteState.MARKED_FOR_REVIEW -> Color(0xFFAB47BC)
    PaletteState.ANSWERED_AND_MARKED -> Color(0xFF7E57C2)
}

private fun PaletteState.label(): String = when (this) {
    PaletteState.NOT_VISITED -> "Not visited"
    PaletteState.NOT_ANSWERED -> "Skipped"
    PaletteState.ANSWERED -> "Answered"
    PaletteState.MARKED_FOR_REVIEW -> "Marked"
    PaletteState.ANSWERED_AND_MARKED -> "Ans. + Marked"
}

@Composable
fun MockTestPalette(
    questions: List<MockTestQuestionEntity>,
    currentIndex: Int,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PaletteState.entries.forEach { state -> PaletteLegendItem(state) }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.height(160.dp),
        ) {
            items(questions.size) { index ->
                val entity = questions[index]
                val isCurrent = index == currentIndex
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                        .background(paletteStateFor(entity).color(), RoundedCornerShape(6.dp))
                        .then(
                            if (isCurrent) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onJump(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteLegendItem(state: PaletteState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(state.color(), RoundedCornerShape(3.dp)))
        Text(state.label(), style = MaterialTheme.typography.labelSmall)
    }
}
