package com.example.cleannotes.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.ui.components.*
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
	notes: List<Note>,
	onDelete: (Note) -> Unit,
	onOpenNote: (Note) -> Unit,
	onSettings: () -> Unit,
	onOpenNotes: () -> Unit,
	onOpenReminders: () -> Unit,
	onCreateNote: () -> Unit,
) {
	var isListView by rememberSaveable { mutableStateOf(true) }
	var noteToDelete by remember { mutableStateOf<Note?>(null) }
	DeleteNoteDialog(noteToDelete, { noteToDelete = null }, onDelete)
	Scaffold(topBar = { CustomTopBar(onSettings) }) { padding ->
		NotesCollection(
			notes = notes,
			isListView = isListView,
			showContent = false,
			onClick = onOpenNote,
			onLongClick = { noteToDelete = it },
			modifier = Modifier.fillMaxSize().padding(padding),
			header = {
				Column {
					Spacer(Modifier.height(16.dp))
					Text(
						DateUtils.fullDate(System.currentTimeMillis()).replaceFirstChar {
							it.titlecase()
						},
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Spacer(Modifier.height(12.dp))
					val reminders = notes.count { it.isReminder }
					val noteCount = notes.size - reminders
					FlowRow(
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						modifier = Modifier.fillMaxWidth(),
					) {
						SuggestionChip(
							onClick = onOpenNotes,
							label = {
								Text(
									pluralStringResource(R.plurals.note_count, noteCount, noteCount)
								)
							},
							icon = { Icon(MindIcons.StickyNote2, null, Modifier.size(16.dp)) },
						)
						SuggestionChip(
							onClick = onOpenReminders,
							label = {
								Text(
									pluralStringResource(
										R.plurals.reminder_count,
										reminders,
										reminders,
									)
								)
							},
							icon = { Icon(MindIcons.Notifications, null, Modifier.size(16.dp)) },
						)
					}
					Row(
						Modifier.fillMaxWidth().padding(top = 16.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							stringResource(R.string.recent_activity),
							style = MaterialTheme.typography.titleLarge,
							modifier = Modifier.weight(1f),
						)
						ViewToggle(isListView) { isListView = !isListView }
					}
					if (notes.isEmpty()) {
						EmptyNotesState(
							stringResource(R.string.empty_notes_title),
							stringResource(R.string.empty_notes_body),
							action = stringResource(R.string.new_note),
							onAction = onCreateNote,
						)
					}
				}
			},
		)
	}
}
