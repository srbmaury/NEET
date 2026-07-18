package com.neet.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.neet.app.domain.Weightage

private fun Weightage.color(): Color = when (this) {
    Weightage.HIGH -> Color(0xFFC62828)
    Weightage.MEDIUM -> Color(0xFFEF6C00)
    Weightage.LOW -> Color(0xFF616161)
}

/**
 * Shared topic-list row — used by the Practice and Notes topic pickers. Both scroll through the
 * same 20+ item TopicCatalog lists, where a full-width FilterChip (built for small pill-shaped
 * selections, not long vertical lists) had no room to show weightage and gave a cramped touch
 * target. [selected] drives the highlighted state for pickers with a select-then-confirm flow
 * (Practice); [showChevron] signals a row that navigates immediately on tap (Notes).
 */
@Composable
fun TopicRow(
    topic: String,
    weightage: Weightage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showChevron: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(topic, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${weightage.name} weightage",
                    style = MaterialTheme.typography.labelSmall,
                    color = weightage.color(),
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
