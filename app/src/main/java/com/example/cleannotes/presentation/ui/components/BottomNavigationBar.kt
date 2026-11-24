package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.notebook.NotesTab

@Composable
fun BottomNavigationBar(
	selectedTab: NotesTab,
	onTabSelected: (NotesTab) -> Unit,
	onCreate: (Boolean) -> Unit,
) {
	var showMenu by remember { mutableStateOf(false) }
	val destinations =
		listOf(
			Triple(NotesTab.HOME, R.string.home, MindIcons.Home),
			Triple(NotesTab.NOTES, R.string.notes, MindIcons.StickyNote2),
			Triple(NotesTab.REMINDERS, R.string.nav_reminders, MindIcons.Notifications),
			Triple(NotesTab.SEARCH, R.string.search, MindIcons.Search),
		)
	Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
		Column {
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
			Row(
				Modifier.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 6.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				destinations.forEachIndexed { index, (route, label, icon) ->
					if (index == 2) {
						Box(Modifier.padding(horizontal = 4.dp)) {
							FilledIconButton(
								onClick = { showMenu = true },
								modifier = Modifier.size(52.dp),
								shape = RoundedCornerShape(18.dp),
							) {
								Icon(
									MindIcons.Add,
									stringResource(R.string.create),
									modifier = Modifier.size(26.dp),
								)
							}
							DropdownMenu(
								expanded = showMenu,
								onDismissRequest = { showMenu = false },
							) {
								DropdownMenuItem(
									text = { Text(stringResource(R.string.new_note)) },
									leadingIcon = {
										Icon(MindIcons.StickyNote2, null)
									},
									onClick = {
										showMenu = false
										onCreate(false)
									},
								)
								DropdownMenuItem(
									text = { Text(stringResource(R.string.new_reminder)) },
									leadingIcon = { Icon(MindIcons.Notifications, null) },
									onClick = {
										showMenu = false
										onCreate(true)
									},
								)
							}
						}
					}
					val selected = selectedTab == route
					NavigationBarItem(
						selected = selected,
						onClick = {
							onTabSelected(route)
						},
						icon = { Icon(icon, stringResource(label)) },
						label = {
							Text(
								stringResource(label),
								maxLines = 1,
								style = MaterialTheme.typography.labelSmall,
							)
						},
						colors =
							NavigationBarItemDefaults.colors(
								selectedIconColor = MaterialTheme.colorScheme.primary,
								selectedTextColor = MaterialTheme.colorScheme.primary,
								indicatorColor = MaterialTheme.colorScheme.primaryContainer,
								unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
								unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
							),
					)
				}
			}
		}
	}
}
