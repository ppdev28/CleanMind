package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.ui.design.MindIcons

@Composable
fun DeleteNoteDialog(note: Note?, onDismiss: () -> Unit, onConfirm: (Note) -> Unit) {
	if (note == null) return
	AlertDialog(
		onDismissRequest = onDismiss,
		icon = { Icon(MindIcons.Delete, null) },
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
				Text(
					stringResource(R.string.delete),
					color = MaterialTheme.colorScheme.error,
				)
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
		},
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
	TopAppBar(
		modifier =
			Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()
				.padding(horizontal = 16.dp, vertical = 8.dp),
		windowInsets = WindowInsets(0.dp),
		title = { Text(title, style = MaterialTheme.typography.titleLarge) },
		navigationIcon = {
			IconButton(onClick = onBack) {
				Icon(
					MindIcons.ArrowBack,
					stringResource(R.string.back),
				)
			}
		},
		actions = actions,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			scrolledContainerColor = MaterialTheme.colorScheme.background,
		),
	)
}

