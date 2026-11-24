package com.example.cleannotes.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.ThemeMode
import com.example.cleannotes.presentation.ui.components.ScreenTopBar
import com.example.cleannotes.presentation.ui.design.MindIcons

@Composable
fun SettingsScreen(
	state: SettingsUiState,
	onThemeChange: (ThemeMode) -> Unit,
	onBack: () -> Unit,
	onRetry: () -> Unit,
	storageContent: @Composable () -> Unit = { StorageSettings() },
) {
	val context = LocalContext.current
	Scaffold(topBar = { ScreenTopBar(stringResource(R.string.settings), onBack) }) { padding ->
		Column(
			Modifier.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.navigationBarsPadding()
				.padding(24.dp),
			verticalArrangement = Arrangement.spacedBy(24.dp),
		) {
			if (state.hasError) {
				Text(
					stringResource(R.string.preferences_error),
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodyMedium,
				)
				TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
			}
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					stringResource(R.string.appearance),
					style = MaterialTheme.typography.headlineSmall,
				)
				Text(
					stringResource(R.string.appearance_body),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
				Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
					listOf(
						ThemeMode.SYSTEM to R.string.theme_system,
						ThemeMode.LIGHT to R.string.theme_light,
						ThemeMode.DARK to R.string.theme_dark,
					)
						.forEach { (mode, label) ->
							Row(
								Modifier.fillMaxWidth()
									.selectable(
										selected = state.themeMode == mode,
										onClick = { onThemeChange(mode) },
										role = Role.RadioButton,
									)
									.padding(horizontal = 20.dp, vertical = 16.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(
									stringResource(label),
									style = MaterialTheme.typography.bodyLarge,
									modifier = Modifier.weight(1f),
								)
								RadioButton(selected = state.themeMode == mode, onClick = null)
							}
						}
				}
			}
			Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
				Column(Modifier.padding(20.dp)) { storageContent() }
			}
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Icon(
					MindIcons.Notifications,
					null,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Column {
					Text(
						"Los avisos se configuran desde cada nota o recordatorio.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					TextButton(
						onClick = {
							context.startActivity(
								if (Build.VERSION.SDK_INT >= 26)
									Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
										.putExtra(
											Settings.EXTRA_APP_PACKAGE,
											context.packageName,
										)
								else
									Intent(
										Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
										Uri.parse("package:${context.packageName}"),
									)
							)
						},
						contentPadding = PaddingValues(0.dp),
					) {
						Text("Ajustes de notificaciones")
					}
				}
			}
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
			Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
				Text(
					stringResource(R.string.app_name),
					style = MaterialTheme.typography.titleMedium,
				)
				Text(
					stringResource(R.string.about_body),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}
