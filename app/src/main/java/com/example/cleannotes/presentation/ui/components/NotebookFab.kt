package com.example.cleannotes.presentation.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.presentation.notebook.NotesTab
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.motion.AppMotion

/** A speed dial anchored to the same FAB: expansion keeps the button in place. */
@Composable
fun NotebookFab(
	selectedTab: NotesTab,
	onTabSelected: (NotesTab) -> Unit,
	onCreate: (Boolean) -> Unit,
	folderName: String? = null,
	onSearchDirect: () -> Unit = {},
	active: Boolean = true,
) {
	var expanded by rememberSaveable { mutableStateOf(false) }
	val rotation by animateFloatAsState(
		if (expanded) 45f else 0f,
		tween(AppMotion.Short),
		label = "fabRotation"
	)
	BackHandler(enabled = active && expanded) { expanded = false }
	LaunchedEffect(active) { if (!active) expanded = false }
	val destinations = listOf(
		Triple(NotesTab.HOME, "Inicio", MindIcons.Home),
		Triple(NotesTab.NOTES, "Notas", MindIcons.StickyNote2),
		Triple(NotesTab.REMINDERS, "Recordar", MindIcons.Notifications),
		Triple(NotesTab.SEARCH, "Buscar", MindIcons.Search),
	)
	Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
		AnimatedVisibility(
			visible = expanded,
			modifier = Modifier.weight(1f, fill = false),
			enter = fadeIn(tween(AppMotion.Short)) + expandVertically(
				tween(AppMotion.Standard),
				expandFrom = Alignment.Bottom
			),
			exit = fadeOut(tween(AppMotion.Short)) + shrinkVertically(
				tween(AppMotion.Standard),
				shrinkTowards = Alignment.Bottom
			),
		) {
			Column(
				Modifier.widthIn(max = 256.dp).verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalAlignment = Alignment.End,
			) {
				destinations.forEach { (route, label, icon) ->
					FabAction(
						label, icon, selected = selectedTab == route,
						onDoubleClick = if (route == NotesTab.SEARCH) ({
							expanded = false; onSearchDirect()
						}) else null,
					) {
						expanded = false
						onTabSelected(route)
					}
				}
				FabAction("Nueva nota", MindIcons.EditNote, subtitle = folderName?.let { "En $it" }) {
					expanded = false
					onCreate(false)
				}
				FabAction("Nuevo recordatorio", MindIcons.Alarm) {
					expanded = false
					onCreate(true)
				}
			}
		}
		FloatingActionButton(
			onClick = { expanded = !expanded },
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
		) {
			Icon(
				MindIcons.Add,
				if (expanded) "Cerrar acciones" else "Crear",
				Modifier.graphicsLayer { rotationZ = rotation })
		}
	}
}

@Composable
private fun FabAction(
	label: String,
	icon: ImageVector,
	subtitle: String? = null,
	selected: Boolean = false,
	onDoubleClick: (() -> Unit)? = null,
	onClick: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxWidth().semantics { this.selected = selected }
			.combinedClickable(role = Role.Button, onClick = onClick, onDoubleClick = onDoubleClick),
		shape = RoundedCornerShape(16.dp),
		shadowElevation = 3.dp,
		color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
	) {
		Row(
			Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 10.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Column(Modifier.weight(1f)) {
				Text(label, style = MaterialTheme.typography.labelLarge)
				if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
			}
			Icon(icon, null)
		}
	}
}
