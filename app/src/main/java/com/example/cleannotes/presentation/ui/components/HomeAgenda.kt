package com.example.cleannotes.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.creationTimestamp
import com.example.cleannotes.presentation.ui.design.*
import com.example.cleannotes.presentation.util.DateUtils
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.cleannotes.presentation.ui.motion.AppMotion
import java.text.SimpleDateFormat
import java.util.Date

@Composable
private fun AgendaDateDivider(at: Long) {
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
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.padding(top = 8.dp).semantics { heading() },
		)
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
	}
}

/** Both layouts retain creation-day dividers and expose only preview metadata. */
@Composable
fun HomeAgenda(
	notes: List<Note>,
	grid: Boolean,
	onOpen: (Note) -> Unit,
	onOptions: (Note) -> Unit,
	onCreate: () -> Unit,
	selectedIds: Set<Int> = emptySet(),
) {
	val gridState = rememberLazyGridState()
	val agendaState = rememberLazyGridState()
	val days = remember(notes) {
		notes.sortedWith(compareByDescending<Note> { it.creationTimestamp() }.thenByDescending { it.id })
			.groupBy { DateUtils.dayKey(it.creationTimestamp()) }
	}
	Crossfade(grid, animationSpec = tween(AppMotion.Short), label = "homeLayout") { gridMode ->
		LazyVerticalGrid(
			columns = GridCells.Adaptive(128.dp),
			state = if (gridMode) gridState else agendaState,
			contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item(key = "agenda-title", span = { GridItemSpan(maxLineSpan) }) {
				Text(
					"Tu agenda",
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.padding(vertical = 8.dp)
				)
			}
			days.forEach { (day, notesOfDay) ->
				item(key = "day-$day", span = { GridItemSpan(maxLineSpan) }) {
					AgendaDateDivider(notesOfDay.first().creationTimestamp())
				}
				items(
					notesOfDay,
					key = { "note-${it.id}" },
					span = { GridItemSpan(if (gridMode) 1 else maxLineSpan) }) { note ->
					DesignNoteCard(
						note,
						Modifier.animateItem(),
						{ onOpen(note) },
						{ onOptions(note) },
						compact = gridMode,
						dateTimestamp = note.creationTimestamp(),
						selectionMode = selectedIds.isNotEmpty(), selected = note.id in selectedIds
					)
				}
			}
			if (notes.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
				Column(
					Modifier.padding(vertical = 32.dp),
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					Text("Un lugar para tus ideas", style = MaterialTheme.typography.titleLarge)
					Text(
						"Todavía no hay notas. Guarda lo que quieras retomar.",
						style = MaterialTheme.typography.bodyMedium
					)
					Button(onClick = onCreate) { Text("Crear una nota") }
				}
			}
		}
	}
}
