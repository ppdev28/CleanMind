package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings

@Composable
fun CustomTopBar(onSettings: () -> Unit, title: String? = null) {
	Row(
		modifier =
			Modifier.fillMaxWidth()
				.statusBarsPadding()
				.padding(start = 24.dp, end = 16.dp, top = 28.dp, bottom = 20.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			title ?: stringResource(R.string.app_name),
			style = MaterialTheme.typography.titleLarge,
			modifier = Modifier.weight(1f).padding(start = 12.dp),
		)
		IconButton(onClick = onSettings) {
			Icon(
				Icons.Outlined.Settings,
				stringResource(R.string.settings),
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}
