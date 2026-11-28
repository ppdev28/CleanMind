package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.ui.design.MindIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
	onSettings: () -> Unit,
	title: String? = null,
	windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
	scrollBehavior: TopAppBarScrollBehavior? = null,
	onToggleLayout: (() -> Unit)? = null,
	gridLayout: Boolean = true,
	layoutLabel: String = "Cambiar vista",
) {
	TopAppBar(
		modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
			.padding(horizontal = 12.dp),
		expandedHeight = 72.dp,
		windowInsets = windowInsets,
		scrollBehavior = scrollBehavior,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			scrolledContainerColor = MaterialTheme.colorScheme.background,
			titleContentColor = MaterialTheme.colorScheme.onSurface,
			actionIconContentColor = MaterialTheme.colorScheme.onSurface,
		),
		title = {
			Row(
				Modifier.padding(start = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				if (title == null) Icon(
					MindIcons.FountainPen,
					null,
					Modifier.size(24.dp),
					tint = MaterialTheme.colorScheme.tertiary
				)
				Text(
					title ?: stringResource(R.string.app_name),
					style = MaterialTheme.typography.titleLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		},
		actions = {
			onToggleLayout?.let { toggle ->
				IconButton(onClick = toggle) {
					Icon(
						if (gridLayout) MindIcons.ViewList else MindIcons.GridView,
						layoutLabel
					)
				}
			}
			IconButton(onClick = onSettings) {
				Icon(MindIcons.Settings, stringResource(R.string.settings))
			}
		},
	)
}
