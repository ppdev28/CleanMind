package com.example.cleannotes.presentation.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.cleannotes.model.ThemeMode
import com.example.cleannotes.presentation.notebook.NotebookScreen
import com.example.cleannotes.presentation.notebook.NotesViewModel
import com.example.cleannotes.presentation.ui.edit_note.EditNoteScreen
import com.example.cleannotes.presentation.ui.edit_note.viewmodel.EditNoteViewModel
import com.example.cleannotes.presentation.ui.motion.AppMotion
import com.example.cleannotes.presentation.ui.settings.SettingsScreen
import com.example.cleannotes.presentation.ui.settings.SettingsViewModel
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme

@Composable
fun NotesApp(
	settingsViewModel: SettingsViewModel = hiltViewModel(),
	notificationNoteId: Int? = null,
	onNotificationOpened: () -> Unit = {},
	onReady: () -> Unit = {},
) {
	val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
	val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
	LaunchedEffect(settings.isLoading) { if (!settings.isLoading) onReady() }
	val dark =
		when (settings.themeMode) {
			ThemeMode.SYSTEM -> isSystemInDarkTheme()
			ThemeMode.LIGHT -> false
			ThemeMode.DARK -> true
		}
	CleanMindTheme(darkTheme = dark) {
		Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
			if (settings.isLoading) {
				Box(
					Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator()
				}
			} else {
				val backStack = rememberNavBackStack(Destination.Notebook)
				LaunchedEffect(notificationNoteId) {
					notificationNoteId?.let { id ->
						val destination = Destination.Editor(noteId = id)
						if (backStack.lastOrNull() != destination) backStack.add(destination)
						onNotificationOpened()
					}
				}
				val open: (Destination) -> Unit = { destination ->
					if (backStack.lastOrNull() != destination) backStack.add(destination)
				}
				val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
				NavDisplay(
					backStack = backStack,
					transitionSpec = { AppMotion.navigation(direction = direction) },
					popTransitionSpec = {
						AppMotion.navigation(forward = false, direction = direction)
					},
					predictivePopTransitionSpec = {
						AppMotion.navigation(forward = false, direction = direction)
					},
					onBack = goBack,
					entryDecorators =
						listOf(
							rememberSaveableStateHolderNavEntryDecorator(),
							rememberViewModelStoreNavEntryDecorator(),
						),
					entryProvider =
						entryProvider {
							entry<Destination.Notebook> {
								val viewModel = hiltViewModel<NotesViewModel>()
								val state by viewModel.uiState.collectAsStateWithLifecycle()
								NotebookScreen(
									state,
									viewModel::onTabSelected,
									viewModel::onQueryChange,
									viewModel::deleteNote,
									viewModel::retry,
									viewModel::dismissError,
									onOpenNote = { open(Destination.Editor(noteId = it.id)) },
									onCreate = { reminder ->
										open(
											Destination.Editor(
												isReminder = reminder,
												folderId =
													if (
														!reminder &&
														state.selectedTab ==
														com.example.cleannotes.presentation
															.notebook
															.NotesTab
															.NOTES
													)
														state.selectedFolderId?.takeIf { it > 0 }
													else null,
											)
										)
									},
									onFolderSelected = viewModel::selectFolder,
									onSaveFolder = viewModel::saveFolder,
									onDeleteFolder = viewModel::deleteFolder,
									onMoveNote = viewModel::moveNote,
									onClearFolderError = viewModel::clearFolderError,
									active = backStack.lastOrNull() == Destination.Notebook,
									onSettings = { open(Destination.Settings) },
									onUpdateNote = viewModel::updateNote,
									onQuickCreate = {
										open(Destination.Editor(quickCapture = true))
									},
								)
							}
							entry<Destination.Settings> {
								SettingsScreen(
									settings,
									settingsViewModel::setThemeMode,
									goBack,
									settingsViewModel::retry,
								)
							}
							entry<Destination.Editor> { destination ->
								val viewModel =
									hiltViewModel<EditNoteViewModel, EditNoteViewModel.Factory>(
										creationCallback = {
											it.create(destination.noteId, destination.isReminder)
										}
									)
								val state by viewModel.uiState.collectAsStateWithLifecycle()
								LaunchedEffect(viewModel, destination.folderId) {
									viewModel.setInitialFolder(destination.folderId)
								}
								LaunchedEffect(state.exitRequested) {
									if (state.exitRequested) goBack()
								}
								if (state.isReminder)
									com.example.cleannotes.presentation.ui.edit_note
										.ReminderEditorScreen(
											state,
											viewModel::onEvent,
											viewModel::saveAndClose,
											goBack,
											active = backStack.lastOrNull() == destination,
										)
								else
									EditNoteScreen(
										state,
										viewModel::onEvent,
										viewModel::saveAndClose,
										goBack,
										destination.quickCapture,
										active = backStack.lastOrNull() == destination,
									)
							}
						},
				)
			}
		}
	}
}
