package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.creationTimestamp
import com.example.cleannotes.presentation.ui.design.DesignNoteCard
import com.example.cleannotes.presentation.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AgendaDayGroup(notes: List<Note>, onOpen: (Note) -> Unit, onOptions: (Note) -> Unit) {
    if (notes.isEmpty()) return
    val at = notes.first().creationTimestamp()
    val locale = LocalConfiguration.current.locales[0]
    val fullDate = SimpleDateFormat(if (locale.language == "es") "EEEE, d 'de' MMMM yyyy" else "EEEE, d MMMM yyyy", locale).format(Date(at)).replaceFirstChar { it.titlecase(locale) }
    val dayLabel = when {
        DateUtils.isToday(at) -> "Hoy"
        DateUtils.isYesterday(at) -> "Ayer"
        else -> SimpleDateFormat("EEEE", locale).format(Date(at)).replaceFirstChar { it.titlecase(locale) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(fullDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.width(56.dp).heightIn(min = 56.dp).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(SimpleDateFormat("dd", locale).format(Date(at)), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(SimpleDateFormat("MMM", locale).format(Date(at)).uppercase(locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(dayLabel, style = MaterialTheme.typography.labelLarge)
                notes.forEach { note ->
                    androidx.compose.runtime.key(note.id) {
                        DesignNoteCard(note, onClick = { onOpen(note) }, onLongClick = { onOptions(note) }, dateTimestamp = note.creationTimestamp())
                    }
                }
            }
        }
    }
}
