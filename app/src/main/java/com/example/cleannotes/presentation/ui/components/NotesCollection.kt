package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.util.DateUtils

@Composable
fun EmptyNotesState(
	title: String,
	body: String,
	modifier: Modifier = Modifier,
	icon: ImageVector = Icons.AutoMirrored.Outlined.StickyNote2,
	action: String? = null,
	onAction: () -> Unit = {},
) {
	Column(
		modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Surface(
			color = MaterialTheme.colorScheme.primaryContainer,
			shape = RoundedCornerShape(28.dp),
		) {
			Icon(
				icon,
				null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.padding(24.dp).size(34.dp),
			)
		}
		Spacer(Modifier.height(24.dp))
		Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
		Spacer(Modifier.height(10.dp))
		Text(
			body,
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		if (action != null) {
			Spacer(Modifier.height(24.dp))
			Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(action) }
		}
	}
}

@Composable
fun DeleteNoteDialog(note: Note?, onDismiss: () -> Unit, onConfirm: (Note) -> Unit) {
	if (note == null) return
	AlertDialog(
		onDismissRequest = onDismiss,
		icon = { Icon(Icons.Outlined.Delete, null) },
		title = {
			Text(
				stringResource(
					if (note.isReminder) R.string.delete_reminder else R.string.delete_note
				)
			)
		},
		text = { Text(stringResource(R.string.delete_message, note.title)) },
		confirmButton = {
			TextButton(
				onClick = {
					onConfirm(note)
					onDismiss()
				}
			) {
				Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
		},
	)
}

@Composable
fun ViewToggle(isListView: Boolean, onClick: () -> Unit) {
	IconButton(onClick = onClick) {
		Icon(
			if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
			stringResource(if (isListView) R.string.view_grid else R.string.view_list),
			tint = MaterialTheme.colorScheme.primary,
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
	TopAppBar(
		title = { Text(title, style = MaterialTheme.typography.titleLarge) },
		navigationIcon = {
			IconButton(onClick = onBack) {
				Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
			}
		},
		actions = actions,
		colors =
			TopAppBarDefaults.topAppBarColors(
				containerColor = MaterialTheme.colorScheme.background
			),
	)
}

@Composable
fun NotesCollection(
	notes: List<Note>,
	isListView: Boolean,
	onClick: (Note) -> Unit,
	onLongClick: (Note) -> Unit,
	modifier: Modifier = Modifier,
	showContent: Boolean = true,
	header: @Composable () -> Unit = {},
) {
	if (isListView) {
		val groups = notes.groupBy { DateUtils.dayKey(it.timestamp) }
		LazyColumn(
			modifier,
			contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(18.dp),
		) {
			item(key = "header") { header() }
			items(groups.entries.toList(), key = { it.key }) { (_, notesOfDay) ->
				HomeNoteItem(
					notes = notesOfDay,
					onClick = onClick,
					onLongClick = onLongClick,
					showContent = showContent,
				)
			}
		}
	} else {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(156.dp),
			modifier = modifier,
			contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
			items(notes, key = { requireNotNull(it.id) }) { note ->
				SimpleNoteItem(
					note = note,
					onClick = { onClick(note) },
					onDelete = { onLongClick(note) },
					compact = true,
					showContent = showContent,
				)
			}
		}
	}
}

@Composable
fun CollectionScreen(
	notes: List<Note>,
	reminders: Boolean,
	onDelete: (Note) -> Unit,
	onBack: () -> Unit,
	onOpenNote: (Note) -> Unit,
	onCreate: () -> Unit,
) {
	var isListView by rememberSaveable { mutableStateOf(true) }
	var noteToDelete by remember { mutableStateOf<Note?>(null) }
	DeleteNoteDialog(noteToDelete, { noteToDelete = null }, onDelete)
	Scaffold(
		topBar = {
			ScreenTopBar(
				stringResource(if (reminders) R.string.reminders else R.string.notes),
				onBack,
			) {
				ViewToggle(isListView) { isListView = !isListView }
			}
		}
	) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			Text(
				stringResource(
					if (reminders) R.string.reminders_subtitle else R.string.all_notes_subtitle
				),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
			)
			if (notes.isEmpty()) {
				EmptyNotesState(
					title =
						stringResource(
							if (reminders) R.string.empty_reminders_title
							else R.string.empty_notes_title
						),
					body =
						stringResource(
							if (reminders) R.string.empty_reminders_body
							else R.string.empty_notes_body
						),
					modifier = Modifier.weight(1f),
					action =
						stringResource(if (reminders) R.string.new_reminder else R.string.new_note),
					onAction = onCreate,
				)
			} else {
				NotesCollection(
					notes,
					isListView,
					onOpenNote,
					{ noteToDelete = it },
					Modifier.weight(1f),
				)
			}
		}
	}
}
