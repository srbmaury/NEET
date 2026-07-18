package com.neet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neet.app.data.local.AnsweredQuestionEntity

private val correctColor = Color(0xFF2E7D32)
private val wrongColor = Color(0xFFC62828)

/**
 * Shared list-item card for an answered question — used by the Progress tab's "Recent answers"
 * list and the "Review Mistakes" per-topic list. A leading correct/wrong icon carries the
 * correctness signal at a glance (rather than making the reader parse colored answer text), and a
 * trailing chevron signals the card opens into the full explanation.
 */
@Composable
fun AnswerRecordCard(
    record: AnsweredQuestionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopic: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (record.isCorrect) correctColor else wrongColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (record.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (record.isCorrect) "Correct" else "Incorrect",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showTopic) {
                    Text(
                        record.topic,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                MarkdownText(
                    markdown = record.stem,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                MarkdownText(
                    markdown = "Your answer: ${record.selectedOptionKey}. ${record.selectedOptionText}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                if (!record.isCorrect) {
                    MarkdownText(
                        markdown = "Correct answer: ${record.correctOptionKey}. ${record.correctOptionText}",
                        style = MaterialTheme.typography.bodySmall.copy(color = correctColor),
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
