package com.example.cleannotes.presentation.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.cleannotes.R
import com.example.cleannotes.model.ThemeMode
import com.example.cleannotes.presentation.ui.design.*

@Composable
fun SettingsScreen(state: SettingsUiState, onThemeChange: (ThemeMode) -> Unit, onBack: () -> Unit, onRetry: () -> Unit, onHome: () -> Unit = onBack) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("capture_preferences", android.content.Context.MODE_PRIVATE) }
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var sound by remember { mutableStateOf(preferences.getBoolean("sound", false)) }
    var reduced by remember { mutableStateOf(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) }
    var advanced by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    Scaffold(contentWindowInsets = WindowInsets.statusBars) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignIconButton(R.drawable.figma_back, "Volver", onClick = onBack)
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
            }
            Text("A tu ritmo", style = MaterialTheme.typography.titleLarge)
            Text("Solo las ayudas que te sirvan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreferenceRow("Notificaciones", notifications) {
                if (!notifications && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
            PreferenceRow("Sonido", sound) { sound = !sound; preferences.edit().putBoolean("sound", sound).apply() }
            PreferenceRow("Animación reducida", reduced) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            DesignRow("Tamaño del texto", R.drawable.figma_arrow, "Usar el tamaño del dispositivo") { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            Text("La app respeta el texto ampliado y la preferencia de movimiento del dispositivo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Clean Notes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) { Text("Volver a Inicio") }
            TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) { Text("Apariencia y almacenamiento") }
            if (advanced) {
                Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) { ThemeMode.SYSTEM -> "Como el dispositivo"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }
                    Surface(onClick = { onThemeChange(mode) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            RadioButton(state.themeMode == mode, onClick = { onThemeChange(mode) })
                        }
                    }
                }
                StorageSettings()
            }
            if (state.hasError) {
                Text("No se pudo guardar la preferencia", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun PreferenceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(if (selected) "✓ Sí" else "— No", style = MaterialTheme.typography.bodyMedium, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
