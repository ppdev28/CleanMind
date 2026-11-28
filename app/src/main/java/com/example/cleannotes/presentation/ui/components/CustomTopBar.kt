package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
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
			.padding(horizontal = 4.dp),
		expandedHeight = 64.dp,
		windowInsets = windowInsets,
		scrollBehavior = scrollBehavior,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			scrolledContainerColor = MaterialTheme.colorScheme.background,
			titleContentColor = MaterialTheme.colorScheme.onSurface,
			actionIconContentColor = MaterialTheme.colorScheme.onSurface,
		),
		title = {
			Text(
				title ?: stringResource(R.string.app_name),
				style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		},
		navigationIcon = {
			if (title == null) Box(
				Modifier.size(48.dp),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					painterResource(R.drawable.ic_launcher_foreground),
					null,
					Modifier.size(24.dp).scale(1.5f),
					tint = MaterialTheme.colorScheme.onSurface
				)
			}
		},
		actions = {
			onToggleLayout?.let { toggle ->
				IconButton(onClick = toggle, modifier = Modifier.size(48.dp)) {
					Icon(
						if (gridLayout) MindIcons.ViewList else MindIcons.GridView,
						layoutLabel
					)
				}
			}
			IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
				Icon(MindIcons.Settings, stringResource(R.string.settings))
			}
		},
	)
}
