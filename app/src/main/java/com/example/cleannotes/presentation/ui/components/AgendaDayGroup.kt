package com.example.cleannotes.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.creationTimestamp
import com.example.cleannotes.presentation.ui.design.DesignNoteCard
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AgendaDayGroup(notes: List<Note>, onOpen: (Note) -> Unit, onOptions: (Note) -> Unit) {
	if (notes.isEmpty()) return
	val at = notes.first().creationTimestamp()
	val locale = LocalConfiguration.current.locales[0]
	val fullDate =
		SimpleDateFormat(
			if (locale.language == "es") "EEEE, d 'de' MMMM yyyy" else "EEEE, d MMMM yyyy",
			locale,
		)
			.format(Date(at))
			.replaceFirstChar { it.titlecase(locale) }
	Column(
		Modifier.fillMaxWidth().animateContentSize(),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Text(
			fullDate,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.semantics { heading() },
		)
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
		notes.forEach { note ->
			androidx.compose.runtime.key(note.id) {
				DesignNoteCard(
					note,
					onClick = { onOpen(note) },
					onLongClick = { onOptions(note) },
					dateTimestamp = note.creationTimestamp(),
				)
			}
		}
	}
}
