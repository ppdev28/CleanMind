package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.cleannotes.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cleannotes.presentation.notebook.NotesTab
import com.example.cleannotes.presentation.ui.design.MindIcons

/** Four destinations and a separate central creation action, attached to the window edge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookBottomBar(
	selectedTab: NotesTab,
	onTabSelected: (NotesTab) -> Unit,
	onCreate: () -> Unit,
	enabled: Boolean = true,
	folderName: String? = null,
) {
	BoxWithConstraints {
		val horizontal = maxWidth >= 600.dp
		val colors = ShortNavigationBarItemDefaults.colors(
			selectedIconColor = MaterialTheme.colorScheme.tertiary,
			selectedTextColor = MaterialTheme.colorScheme.onSurface,
			selectedIndicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
			unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
			unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		ShortNavigationBar(
			containerColor = MaterialTheme.colorScheme.surfaceContainer,
			arrangement = ShortNavigationBarArrangement.EqualWeight,
		) {
			listOf(
				NotesTab.HOME,
				NotesTab.NOTES,
				null,
				NotesTab.REMINDERS,
				NotesTab.SEARCH
			).forEach { tab ->
				if (tab == null) {
					Surface(
						onClick = onCreate,
						enabled = enabled,
						color = androidx.compose.ui.graphics.Color.Transparent,
						modifier = Modifier.alpha(if (enabled) 1f else 0.38f).semantics {
							contentDescription = "Nueva nota"
							if (folderName != null) stateDescription = "Notas nuevas en $folderName"
						},
					) {
						Column(
							Modifier.heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
						) {
							Box(
								Modifier.size(52.dp, 32.dp)
									.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
								contentAlignment = Alignment.Center,
							) { Icon(MindIcons.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
							Text(
								"Nueva nota",
								style = MaterialTheme.typography.labelSmall,
								textAlign = TextAlign.Center,
								maxLines = 2
							)
						}
					}
				} else {
					val label = when (tab) {
						NotesTab.HOME -> stringResource(R.string.home)
						NotesTab.NOTES -> stringResource(R.string.notes)
						NotesTab.REMINDERS -> stringResource(R.string.nav_reminders)
						NotesTab.SEARCH -> stringResource(R.string.search)
					}
					val icon = when (tab) {
						NotesTab.HOME -> MindIcons.Home
						NotesTab.NOTES -> MindIcons.StickyNote2
						NotesTab.REMINDERS -> MindIcons.Notifications
						NotesTab.SEARCH -> MindIcons.Search
					}
					ShortNavigationBarItem(
						selected = selectedTab == tab,
						onClick = { onTabSelected(tab) },
						icon = { Icon(icon, null) },
						label = {
							Text(
								label,
								style = MaterialTheme.typography.labelSmall,
								textAlign = TextAlign.Center,
								maxLines = 2
							)
						},
						modifier = Modifier.semantics { contentDescription = label },
						enabled = enabled,
						iconPosition = if (horizontal) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
						colors = colors,
					)
				}
			}
		}
	}
}
