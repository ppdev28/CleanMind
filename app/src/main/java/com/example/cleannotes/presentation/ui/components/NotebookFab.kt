package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.notebook.NotesTab
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.design.appSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookFab(
	selectedTab: NotesTab,
	onTabSelected: (NotesTab) -> Unit,
	onCreate: (Boolean) -> Unit,
	folderName: String? = null,
	onSearchDirect: () -> Unit = {},
) {
	var showMenu by rememberSaveable { mutableStateOf(false) }
	val destinations =
		listOf(
			Triple(NotesTab.HOME, R.string.home, MindIcons.Home),
			Triple(NotesTab.NOTES, R.string.notes, MindIcons.StickyNote2),
			Triple(NotesTab.REMINDERS, R.string.nav_reminders, MindIcons.Notifications),
			Triple(NotesTab.SEARCH, R.string.search, MindIcons.Search),
		)
	FloatingActionButton(
		onClick = { showMenu = true },
		containerColor = MaterialTheme.colorScheme.primaryContainer,
		contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
	) {
		Icon(MindIcons.Add, stringResource(R.string.create))
	}
	if (showMenu)
		AppBottomSheet(
			onDismissRequest = { showMenu = false },
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		) {
			Column(
				Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
						Text("Tu libreta", style = MaterialTheme.typography.titleLarge)
						Text(
							"Un espacio para tus ideas y pendientes",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
					IconButton(onClick = { showMenu = false }) { Icon(MindIcons.Close, "Cerrar") }
				}
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
					destinations.forEach { (route, label, icon) ->
						Surface(
							modifier = Modifier.weight(1f).combinedClickable(
								role = Role.Button,
								onClick = { showMenu = false; onTabSelected(route) },
								onDoubleClick = if (route == NotesTab.SEARCH) ({ showMenu = false; onSearchDirect() }) else null,
							),
							shape = RoundedCornerShape(16.dp),
							color = if (selectedTab == route) MaterialTheme.colorScheme.primaryContainer
							else MaterialTheme.colorScheme.surfaceContainer,
						) {
							Column(
								Modifier.padding(vertical = 12.dp),
								horizontalAlignment = Alignment.CenterHorizontally,
								verticalArrangement = Arrangement.spacedBy(6.dp),
							) {
								Icon(icon, null)
								Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
							}
						}
					}
				}
				CreationOption(
					"Nueva nota",
					folderName?.let { "Guardar en $it" }
						?: "Escribe, añade imágenes y organiza tus ideas",
					MindIcons.EditNote,
				) {
					showMenu = false
					onCreate(false)
				}
				CreationOption(
					"Nuevo recordatorio",
					"Una tarea, una fecha y un aviso",
					MindIcons.Alarm,
				) {
					showMenu = false
					onCreate(true)
				}
			}
		}
}

@Composable
private fun CreationOption(
	title: String,
	description: String,
	icon: ImageVector,
	onClick: () -> Unit,
) {
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(20.dp),
		color = Color.Transparent,
		modifier = Modifier.appSurface(RoundedCornerShape(20.dp)),
	) {
		Row(
			Modifier.fillMaxWidth().padding(20.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Surface(
				shape = RoundedCornerShape(14.dp),
				color = MaterialTheme.colorScheme.surfaceContainerHigh,
			) {
				Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(icon, null) }
			}
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(title, style = MaterialTheme.typography.titleSmall)
				Text(
					description,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			Icon(MindIcons.Arrow, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
		}
	}
}
