package com.example.cleannotes.presentation.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleannotes.presentation.notebook.NotesTab
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.motion.AppMotion
import kotlin.math.cos
import kotlin.math.sin

/** Two concentric quarter arcs keep all six actions reachable on a 320 dp phone. */
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
	val progress by animateFloatAsState(
		if (expanded) 1f else 0f,
		tween(AppMotion.Standard),
		label = "radialFab"
	)
	BackHandler(enabled = active && expanded) { expanded = false }
	LaunchedEffect(active) { if (!active) expanded = false }
	val visible = expanded || progress > 0f
	Box(Modifier.size(if (visible) 288.dp else 72.dp)) {
		if (visible) {
			val actions = listOf(
				RadialAction(
					"Inicio",
					"Inicio",
					MindIcons.Home,
					216f,
					0f,
					selectedTab == NotesTab.HOME
				) { onTabSelected(NotesTab.HOME) },
				RadialAction(
					"Notas",
					"Notas",
					MindIcons.StickyNote2,
					216f,
					45f,
					selectedTab == NotesTab.NOTES
				) { onTabSelected(NotesTab.NOTES) },
				RadialAction(
					"Recordar",
					"Recordar",
					MindIcons.Notifications,
					216f,
					90f,
					selectedTab == NotesTab.REMINDERS
				) { onTabSelected(NotesTab.REMINDERS) },
				RadialAction(
					"Buscar",
					"Buscar",
					MindIcons.Search,
					108f,
					0f,
					selectedTab == NotesTab.SEARCH
				) { onTabSelected(NotesTab.SEARCH) },
				RadialAction(
					"Nueva nota",
					"Nueva\nnota",
					MindIcons.EditNote,
					108f,
					90f
				) { onCreate(false) },
				RadialAction("Nuevo recordatorio", "Nuevo\naviso", MindIcons.Alarm, 108f, 45f) {
					onCreate(
						true
					)
				},
			)
			actions.forEach { action ->
				val radians = Math.toRadians(action.angle.toDouble())
				Surface(
					modifier = Modifier.align(Alignment.BottomEnd)
						.offset(
							x = (-action.radius * cos(radians) * progress).toFloat().dp,
							y = (-action.radius * sin(radians) * progress).toFloat().dp
						)
						.size(72.dp).graphicsLayer {
							alpha = progress
							scaleX = 0.6f + 0.4f * progress
							scaleY = scaleX
						}.semantics {
							contentDescription = action.label
							selected = action.selected
							if (action.label == "Nueva nota" && folderName != null) stateDescription =
								"Guardar en $folderName"
						}.combinedClickable(
							enabled = expanded,
							role = Role.Button,
							onClick = { expanded = false; action.onClick() },
							onDoubleClick = if (action.label == "Buscar") ({
								expanded = false; onSearchDirect()
							}) else null,
						),
					shape = CircleShape,
					shadowElevation = 3.dp,
					color = if (action.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
				) {
					Column(
						Modifier.padding(6.dp).clearAndSetSemantics { },
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
					) {
						Icon(action.icon, null, Modifier.size(22.dp))
						Text(
							action.shortLabel,
							style = MaterialTheme.typography.labelSmall,
							fontSize = 11.sp,
							lineHeight = 13.sp,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							textAlign = TextAlign.Center
						)
					}
				}
			}
		}
		FloatingActionButton(
			onClick = { expanded = !expanded },
			modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
		) {
			Icon(
				MindIcons.Add,
				if (expanded) "Cerrar acciones" else "Crear",
				Modifier.graphicsLayer { rotationZ = progress * 45f })
		}
	}
}

private data class RadialAction(
	val label: String,
	val shortLabel: String,
	val icon: ImageVector,
	val radius: Float,
	val angle: Float,
	val selected: Boolean = false,
	val onClick: () -> Unit,
)
