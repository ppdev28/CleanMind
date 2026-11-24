package com.example.cleannotes.presentation.ui.edit_note

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.cleannotes.R
import com.example.cleannotes.data.reminders.ReminderScheduler
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.ReminderConfig
import java.text.DateFormat
import java.util.Calendar

@Composable
fun ReminderOptions(document: NoteDocument, enabled: Boolean, onChange: (String) -> Unit) {
	val context = LocalContext.current
	val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
	val dialogContext =
		remember(context, dark) {
			android.view.ContextThemeWrapper(
				context,
				if (dark) R.style.Theme_CleanMind_Dialog_Dark
				else R.style.Theme_CleanMind_Dialog_Light,
			)
		}
	val config = document.reminder
	var allowed by remember {
		mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
	}
	var exact by remember { mutableStateOf(ReminderScheduler.exactAllowed(context)) }
	LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
		allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
		exact = ReminderScheduler.exactAllowed(context)
	}
	val permission =
		rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
			allowed = it
		}

	fun update(value: ReminderConfig?) = onChange(document.copy(reminder = value).encode())
	Column(
		Modifier.fillMaxWidth().padding(bottom = 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
				"Notificación del sistema",
				Modifier.weight(1f),
				style = MaterialTheme.typography.titleSmall,
			)
			Switch(
				checked = config != null,
				enabled = enabled,
				onCheckedChange = { checked ->
					update(
						if (checked) ReminderConfig(System.currentTimeMillis() + 3_600_000)
						else null
					)
					if (
						checked &&
						Build.VERSION.SDK_INT >= 33 &&
						context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
						PackageManager.PERMISSION_GRANTED
					)
						permission.launch(Manifest.permission.POST_NOTIFICATIONS)
				},
			)
		}
		if (config != null) {
			if (!allowed) {
				Text(
					"Las notificaciones están desactivadas. Actívalas para recibir el aviso.",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall,
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
					}
				) {
					Text("Permitir notificaciones")
				}
			}
			Row {
				OutlinedButton(
					enabled = enabled,
					onClick = {
						val c = Calendar.getInstance().apply { timeInMillis = config.at }
						DatePickerDialog(
							dialogContext,
							{ _, year, month, day ->
								c.set(year, month, day)
								update(config.copy(at = c.timeInMillis))
							},
							c.get(Calendar.YEAR),
							c.get(Calendar.MONTH),
							c.get(Calendar.DAY_OF_MONTH),
						)
							.show()
					},
				) {
					Text(DateFormat.getDateInstance().format(config.at))
				}
				Spacer(Modifier.width(8.dp))
				OutlinedButton(
					enabled = enabled,
					onClick = {
						val c = Calendar.getInstance().apply { timeInMillis = config.at }
						TimePickerDialog(
							dialogContext,
							{ _, hour, minute ->
								c.set(Calendar.HOUR_OF_DAY, hour)
								c.set(
									Calendar.MINUTE,
									minute,
								)
								c.set(Calendar.SECOND, 0)
								c.set(Calendar.MILLISECOND, 0)
								update(config.copy(at = c.timeInMillis))
							},
							c.get(Calendar.HOUR_OF_DAY),
							c.get(Calendar.MINUTE),
							android.text.format.DateFormat.is24HourFormat(context),
						)
							.show()
					},
				) {
					Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(config.at))
				}
			}
			if (config.at <= System.currentTimeMillis() && config.repeatUnit == "none")
				Text(
					"Elige una fecha y hora futuras para activar el aviso.",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall,
				)
			OutlinedTextField(
				value = config.message,
				onValueChange = { update(config.copy(message = it)) },
				enabled = enabled,
				label = { Text("Mensaje de la notificación") },
				modifier = Modifier.fillMaxWidth(),
				minLines = 2,
			)
			Text("Repetición", style = MaterialTheme.typography.labelLarge)
			Row(
				Modifier.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				listOf(
					"none" to "Una vez",
					"minute" to "Minutos",
					"hour" to "Horas",
					"day" to "Días",
					"week" to "Semanas",
					"month" to "Meses",
				)
					.forEach { (unit, label) ->
						FilterChip(
							selected = config.repeatUnit == unit,
							enabled = enabled,
							onClick = { update(config.copy(repeatUnit = unit)) },
							label = { Text(label) },
						)
					}
			}
			if (config.repeatUnit != "none") {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text("Cada ${config.interval}", Modifier.weight(1f))
					IconButton(
						enabled = enabled && config.interval > 1,
						onClick = { update(config.copy(interval = config.interval - 1)) },
					) {
						Text("−")
					}
					IconButton(
						enabled = enabled && config.interval < 365,
						onClick = { update(config.copy(interval = config.interval + 1)) },
					) {
						Text("+")
					}
				}
			}
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					"Posponer: ${config.snoozeMinutes} min",
					Modifier.weight(1f),
					style = MaterialTheme.typography.bodyMedium,
				)
				IconButton(
					enabled = enabled && config.snoozeMinutes > 1,
					onClick = { update(config.copy(snoozeMinutes = config.snoozeMinutes - 1)) },
				) {
					Text("−")
				}
				IconButton(
					enabled = enabled && config.snoozeMinutes < 180,
					onClick = { update(config.copy(snoozeMinutes = config.snoozeMinutes + 1)) },
				) {
					Text("+")
				}
			}
			listOf(
				"Sonido" to config.sound,
				"Vibración" to config.vibrate,
				"Prioridad alta" to config.highPriority,
			)
				.forEachIndexed { index, (label, checked) ->
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							label,
							Modifier.weight(1f),
							style = MaterialTheme.typography.bodyMedium,
						)
						Switch(
							checked = checked,
							enabled = enabled,
							onCheckedChange = { value ->
								update(
									when (index) {
										0 -> config.copy(sound = value)
										1 -> config.copy(vibrate = value)
										else -> config.copy(highPriority = value)
									}
								)
							},
						)
					}
				}
			TextButton(
				onClick = {
					context.startActivity(
						if (Build.VERSION.SDK_INT >= 26)
							Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
								.putExtra(
									Settings.EXTRA_APP_PACKAGE,
									context.packageName,
								)
								.putExtra(
									Settings.EXTRA_CHANNEL_ID,
									ReminderScheduler.ensureChannel(context, config),
								)
						else
							Intent(
								Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
								Uri.parse("package:${context.packageName}"),
							)
					)
				}
			) {
				Text("Personalizar tono en Android")
			}
			if (!exact && Build.VERSION.SDK_INT >= 31) {
				Text(
					"Sin permiso de alarmas exactas Android puede retrasar el aviso.",
					style = MaterialTheme.typography.bodySmall,
				)
				TextButton(
					onClick = {
						context.startActivity(
							Intent(
								Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
								Uri.parse("package:${context.packageName}"),
							)
						)
					}
				) {
					Text("Permitir avisos exactos")
				}
			}
		}
	}
}
