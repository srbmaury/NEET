package com.neet.app.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Shows a countdown to the student's NEET exam date plus current syllabus coverage — deliberately
 * not a "you're on pace for X%" projection, since the app has no study-start-date tracked and a
 * fabricated pace number would be worse than two honest figures shown side by side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownCard(
    examDateEpochDay: Long?,
    coveragePercent: Int,
    onExamDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (examDateEpochDay == null) {
                Text("Countdown to NEET", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Set your exam date to see a countdown and syllabus coverage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Set your NEET exam date")
                }
            } else {
                val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(examDateEpochDay))
                Text(
                    if (daysRemaining >= 0) "$daysRemaining days until NEET" else "NEET exam date has passed",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$coveragePercent% of syllabus covered so far",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = examDateEpochDay?.let {
                LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val epochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        onExamDateSelected(epochDay)
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
