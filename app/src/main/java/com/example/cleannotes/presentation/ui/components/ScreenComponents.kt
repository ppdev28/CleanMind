package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.ui.design.MindIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
	TopAppBar(
		modifier =
			Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()
				.padding(horizontal = 16.dp, vertical = 8.dp),
		windowInsets = WindowInsets(0.dp),
		title = { Text(title, style = MaterialTheme.typography.titleLarge) },
		navigationIcon = {
			IconButton(onClick = onBack) {
				Icon(
					MindIcons.ArrowBack,
					stringResource(R.string.back),
				)
			}
		},
		actions = actions,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			scrolledContainerColor = MaterialTheme.colorScheme.background,
		),
	)
}

