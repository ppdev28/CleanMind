package com.example.cleannotes.presentation.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.notebook.NotesUiState
import com.example.cleannotes.presentation.ui.components.*

@Composable
fun SearchScreen(
	state: NotesUiState,
	onQueryChange: (String) -> Unit,
	onDelete: (Note) -> Unit,
	onBack: () -> Unit,
	onOpenNote: (Note) -> Unit,
) {
	var noteToDelete by remember { mutableStateOf<Note?>(null) }
	val term = state.query.trim()
	val results = state.searchResults
	DeleteNoteDialog(noteToDelete, { noteToDelete = null }, onDelete)
	Scaffold(topBar = { ScreenTopBar(stringResource(R.string.search), onBack) }) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			SearchBar(state.query, onQueryChange)
			if (results.isEmpty()) {
				EmptyNotesState(
					stringResource(
						if (term.isEmpty()) R.string.search_title else R.string.no_results
					),
					stringResource(
						if (term.isEmpty()) R.string.search_body else R.string.no_results_body
					),
					modifier = Modifier.weight(1f),
					icon = Icons.Outlined.Search,
				)
			} else {
				Text(
					pluralStringResource(R.plurals.result_count, results.size, results.size),
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
				)
				NotesCollection(
					results,
					true,
					onOpenNote,
					{ noteToDelete = it },
					Modifier.weight(1f),
				)
			}
		}
	}
}
