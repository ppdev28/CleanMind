package com.example.cleannotes.presentation.ui.edit_note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.ReminderConfig
import com.example.cleannotes.presentation.ui.design.*
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
	title: String,
	document: NoteDocument,
	onConfirm: (NoteDocument) -> Unit,
	onDismiss: () -> Unit,
) {
	fun time(tomorrow: Boolean) =
		Calendar.getInstance()
			.apply {
				if (tomorrow) add(Calendar.DAY_OF_YEAR, 1)
				set(Calendar.HOUR_OF_DAY, if (tomorrow) 9 else 18)
				set(Calendar.MINUTE, 0)
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
				if (!tomorrow && timeInMillis <= System.currentTimeMillis())
					add(Calendar.DAY_OF_YEAR, 1)
			}
			.timeInMillis

	val context = androidx.compose.ui.platform.LocalContext.current
	val defaultSound = remember {
		context
			.getSharedPreferences("capture_preferences", android.content.Context.MODE_PRIVATE)
			.getBoolean("sound", false)
	}
	val later = remember { time(false) }
	val tomorrow = remember { time(true) }
	var draft by
	remember(document) {
		mutableStateOf(
			document.copy(
				reminder = document.reminder ?: ReminderConfig(tomorrow, sound = defaultSound),
				reminderCompleted = false,
			)
		)
	}
	var custom by remember { mutableStateOf(false) }
	var advanced by remember { mutableStateOf(false) }
	val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
	val dialogContext =
		remember(context, dark) {
			android.view.ContextThemeWrapper(
				context,
				if (dark) R.style.Theme_CleanMind_Dialog_Dark
				else R.style.Theme_CleanMind_Dialog_Light,
			)
		}
	val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
	val formatter = remember(locale) { SimpleDateFormat("d MMM yyyy · HH:mm", locale) }
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		containerColor = MaterialTheme.colorScheme.background,
	) {
		Column(
			Modifier.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp)
				.padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					if (custom) "Fecha y hora" else "Añadir recordatorio",
					Modifier.weight(1f),
					style = MaterialTheme.typography.headlineSmall,
				)
				DesignIconButton(R.drawable.figma_close, "Cerrar recordatorio", onClick = onDismiss)
			}
			if (!custom) {
				Text(
					"Para «${title.ifBlank { "Sin título" }}»",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				DesignRow(
					"Más tarde",
					R.drawable.figma_clock,
					formatter.format(later),
					draft.reminder?.at == later,
				) {
					custom = false
					draft = draft.copy(reminder = draft.reminder?.copy(at = later))
				}
				DesignRow(
					"Mañana",
					R.drawable.figma_bell,
					formatter.format(tomorrow),
					draft.reminder?.at == tomorrow,
				) {
					custom = false
					draft = draft.copy(reminder = draft.reminder?.copy(at = tomorrow))
				}
				DesignRow("Elegir fecha y hora", R.drawable.figma_arrow, selected = custom) {
					custom = true
				}
			}
			if (custom) {
				val at = draft.reminder?.at ?: tomorrow
				DesignRow(
					"Fecha",
					R.drawable.figma_bell,
					java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, locale)
						.format(at),
				) {
					val calendar = Calendar.getInstance().apply { timeInMillis = at }
					android.app
						.DatePickerDialog(
							dialogContext,
							{ _, year, month, day ->
								calendar.set(year, month, day)
								draft =
									draft.copy(
										reminder = draft.reminder?.copy(at = calendar.timeInMillis)
									)
							},
							calendar.get(Calendar.YEAR),
							calendar.get(Calendar.MONTH),
							calendar.get(Calendar.DAY_OF_MONTH),
						)
						.show()
				}
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					(0..2).forEach { offset ->
						val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
						val selected = Calendar.getInstance().apply { timeInMillis = at }
						FilterChip(
							selected =
								day.get(Calendar.DAY_OF_YEAR) ==
										selected.get(Calendar.DAY_OF_YEAR) &&
										day.get(Calendar.YEAR) == selected.get(Calendar.YEAR),
							onClick = {
								selected.set(
									day.get(Calendar.YEAR),
									day.get(Calendar.MONTH),
									day.get(Calendar.DAY_OF_MONTH),
								)
								draft =
									draft.copy(
										reminder = draft.reminder?.copy(at = selected.timeInMillis)
									)
							},
							label = { Text(SimpleDateFormat("EEE d", locale).format(day.time)) },
							modifier = Modifier.weight(1f),
						)
					}
				}
				DesignRow(
					"Hora",
					R.drawable.figma_clock,
					SimpleDateFormat("HH:mm", locale).format(at),
				) {
					val calendar = Calendar.getInstance().apply { timeInMillis = at }
					android.app
						.TimePickerDialog(
							dialogContext,
							{ _, hour, minute ->
								calendar.set(Calendar.HOUR_OF_DAY, hour)
								calendar.set(Calendar.MINUTE, minute)
								calendar.set(Calendar.SECOND, 0)
								calendar.set(Calendar.MILLISECOND, 0)
								draft =
									draft.copy(
										reminder = draft.reminder?.copy(at = calendar.timeInMillis)
									)
							},
							calendar.get(Calendar.HOUR_OF_DAY),
							calendar.get(Calendar.MINUTE),
							true,
						)
						.show()
				}
				Text(
					"Hora local · Formato de 24 horas",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			if (custom)
				TextButton(onClick = { custom = false }) { Text("Volver a las opciones rápidas") }
			TextButton(onClick = { advanced = !advanced }) {
				Text("Repetición, mensaje y notificación")
			}
			if (advanced) ReminderOptions(draft, true) { draft = NoteDocument.decode(it) }
			if (
				!com.example.cleannotes.data.reminders.ReminderScheduler.exactAllowed(context) &&
				android.os.Build.VERSION.SDK_INT >= 31
			) {
				Text(
					"Android puede retrasar el aviso sin permiso de alarmas exactas.",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				TextButton(
					onClick = {
						context.startActivity(
							android.content.Intent(
								android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
								android.net.Uri.parse("package:${context.packageName}"),
							)
						)
					}
				) {
					Text("Permitir avisos exactos")
				}
			}
			Text(
				"Te lo recordaré",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Text(
				draft.reminder?.at?.let(formatter::format) ?: "Sin aviso",
				style = MaterialTheme.typography.titleSmall,
			)
			Spacer(Modifier.height(16.dp))
			Button(
				onClick = { onConfirm(draft) },
				enabled =
					draft.reminder == null ||
							draft.reminder!!.at > System.currentTimeMillis() ||
							draft.reminder!!.repeatUnit != "none",
				shape = RoundedCornerShape(16.dp),
				modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
			) {
				Text("Confirmar recordatorio")
			}
			if (document.reminder != null)
				TextButton(
					onClick = {
						onConfirm(document.copy(reminder = null, reminderCompleted = false))
					},
					modifier = Modifier.fillMaxWidth(),
				) {
					Text("Quitar recordatorio")
				}
		}
	}
}
