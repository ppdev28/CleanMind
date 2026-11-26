package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
) {
	TopAppBar(
		modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
			.padding(horizontal = 12.dp),
		expandedHeight = 72.dp,
		windowInsets = windowInsets,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			scrolledContainerColor = MaterialTheme.colorScheme.background,
			titleContentColor = MaterialTheme.colorScheme.onSurface,
			actionIconContentColor = MaterialTheme.colorScheme.onSurface,
		),
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
