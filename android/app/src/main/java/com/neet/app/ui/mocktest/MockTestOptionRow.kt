package com.neet.app.ui.mocktest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neet.app.data.model.QuestionOption
import com.neet.app.ui.components.MarkdownText

/**
 * Lightweight option row for *taking* a mock test live — unlike [com.neet.app.ui.question.OptionRow],
 * it never reveals correctness, only the current selection, and stays clickable regardless of
 * whether the question has already been answered (re-selecting is allowed until submit).
 */
@Composable
fun MockTestOptionRow(
    option: QuestionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        MarkdownText(
            markdown = "${option.key}. ${option.text}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}
