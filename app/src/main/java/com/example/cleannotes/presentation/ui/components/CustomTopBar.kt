package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.ui.design.MindIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
	onSettings: () -> Unit,
	title: String? = null,
	scrollBehavior: TopAppBarScrollBehavior? = null,
	windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
	TopAppBar(
		modifier = Modifier.padding(horizontal = 12.dp),
		expandedHeight = 72.dp,
		windowInsets = windowInsets,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = Color.Transparent,
			scrolledContainerColor = Color.Transparent,
			titleContentColor = MaterialTheme.colorScheme.onSurface,
			actionIconContentColor = MaterialTheme.colorScheme.onSurface,
		),
		scrollBehavior = scrollBehavior,
		title = {
			Text(
				title ?: stringResource(R.string.app_name),
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.padding(start = 12.dp),
			)
		},
		actions = {
			IconButton(onClick = onSettings) {
				Icon(MindIcons.Settings, stringResource(R.string.settings))
			}
		},
	)
}
