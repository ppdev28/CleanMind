package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.presentation.ui.design.MindIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
	TopAppBar(
		modifier =
			Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()
				.padding(horizontal = 4.dp),
		expandedHeight = 64.dp,
		windowInsets = WindowInsets(0.dp),
		title = {
			Text(
				title,
				style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		},
		navigationIcon = {
			IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
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

