package com.example.cleannotes.presentation.ui.edit_note

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.data.reminders.ReminderScheduler
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.ReminderConfig
import com.example.cleannotes.presentation.ui.components.ContentSheet
import com.example.cleannotes.presentation.ui.components.ScreenTopBar
import com.example.cleannotes.presentation.ui.design.DesignRow
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.motion.AppMotion
import java.text.DateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(
	state: EditNoteUiState,
	onEvent: (EditNoteEvent) -> Unit,
	onSaveAndClose: () -> Unit,
	onBack: () -> Unit,
	active: Boolean = true,
) {
	val context = LocalContext.current
	val locale = LocalConfiguration.current.locales[0]
	val doc = remember(state.content) { NoteDocument.decode(state.content) }
	val defaultTime = remember {
		Calendar.getInstance()
			.apply {
				add(Calendar.DAY_OF_YEAR, 1)
				set(Calendar.HOUR_OF_DAY, 9)
				set(
					Calendar.MINUTE,
					0,
				)
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}
			.timeInMillis
	}
	val config = doc.reminder ?: ReminderConfig(defaultTime, message = doc.text)
	val message = doc.text.ifBlank { doc.reminder?.message.orEmpty() }
	val existing = remember { state.saveStatus == SaveStatus.SAVED }
	var notify by rememberSaveable { mutableStateOf(doc.reminder != null || !existing) }
	var advanced by rememberSaveable { mutableStateOf(false) }
	var discard by remember { mutableStateOf(false) }
	var denied by remember { mutableStateOf(false) }
	val enabled = !state.isClosing && !state.isLoading && !state.loadFailed
	val validTime = !notify || config.at > System.currentTimeMillis() || config.repeatUnit != "none"
	val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
	val dialogContext =
		remember(context, dark) {
			android.view.ContextThemeWrapper(
				context,
				if (dark) R.style.Theme_CleanMind_Dialog_Dark
				else R.style.Theme_CleanMind_Dialog_Light,
			)
		}

	fun change(next: ReminderConfig) =
		onEvent(
			EditNoteEvent.EnteredContent(
				doc.copy(
					reminder = if (notify) next else null,
					reminderCompleted = false,
				)
					.encode()
			)
		)

	fun save() {
		onEvent(
			EditNoteEvent.EnteredContent(
				doc.copy(
					text = message,
					reminder = if (notify) config.copy(message = message) else null,
					reminderCompleted = false,
				)
					.encode()
			)
		)
		onSaveAndClose()
	}

	val permission =
		rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
			if (allowed) save() else denied = true
		}
	val close: () -> Unit = { if (state.hasChanges) discard = true else onBack() }
	BackHandler(enabled = active, onBack = close)
	if (discard)
		AlertDialog(
			onDismissRequest = { discard = false },
			title = { Text("¿Descartar los cambios?") },
			text = { Text("Este recordatorio no se ha guardado todavía.") },
			confirmButton = {
				TextButton(onClick = onBack) {
					Text(
						"Descartar",
						color = MaterialTheme.colorScheme.error,
					)
				}
			},
			dismissButton = {
				TextButton(onClick = { discard = false }) { Text("Seguir editando") }
			},
		)
	if (advanced)
		AppBottomSheet(
			onDismissRequest = { advanced = false },
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		) {
			Column(
				Modifier.verticalScroll(rememberScrollState())
					.padding(horizontal = 24.dp)
					.padding(bottom = 24.dp)
			) {
				ReminderOptions(doc.copy(reminder = config), enabled) { value ->
					val next = NoteDocument.decode(value)
					notify = next.reminder != null
					onEvent(
						EditNoteEvent.EnteredContent(
							next.copy(text = next.reminder?.message ?: doc.text).encode()
						)
					)
				}
			}
		}
	Scaffold(
		containerColor = androidx.compose.ui.graphics.Color.Transparent,
		topBar = {
			ScreenTopBar(
				if (existing) "Editar recordatorio" else "Nuevo recordatorio",
				close,
			)
		},
		bottomBar = {
			Surface(color = MaterialTheme.colorScheme.surface) {
				Button(
					onClick = {
						if (
							notify &&
							Build.VERSION.SDK_INT >= 33 &&
							context.checkSelfPermission(
								Manifest.permission.POST_NOTIFICATIONS
							) != PackageManager.PERMISSION_GRANTED
						)
							permission.launch(Manifest.permission.POST_NOTIFICATIONS)
						else save()
					},
					enabled = enabled && state.title.isNotBlank() && validTime,
					modifier =
						Modifier.fillMaxWidth()
							.navigationBarsPadding()
							.imePadding()
							.padding(24.dp)
							.heightIn(min = 52.dp),
					shape = RoundedCornerShape(16.dp),
				) {
					Text(if (state.isClosing) "Guardando…" else "Guardar recordatorio")
				}
			}
		},
	) { padding ->
		ContentSheet(padding) {
			Column(
				Modifier.fillMaxSize()
					.animateContentSize()
					.verticalScroll(rememberScrollState())
					.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(20.dp),
			) {
				Text("Una cosa menos en tu cabeza", style = MaterialTheme.typography.titleLarge)
				OutlinedTextField(
					state.title,
					{ onEvent(EditNoteEvent.EnteredTitle(it)) },
					enabled = enabled,
					label = { Text("¿Qué quieres recordar?") },
					modifier = Modifier.fillMaxWidth(),
					minLines = 2,
					shape = RoundedCornerShape(16.dp),
				)
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						MindIcons.Notifications,
						null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Text(
						"Avisarme",
						Modifier.weight(1f).padding(start = 12.dp),
						style = MaterialTheme.typography.bodyMedium,
					)
					Switch(
						notify,
						{ value ->
							notify = value
							onEvent(
								EditNoteEvent.EnteredContent(
									doc.copy(text = message, reminder = if (value) config else null)
										.encode()
								)
							)
						},
						enabled = enabled,
					)
				}
				AnimatedVisibility(notify, enter = AppMotion.reveal(), exit = AppMotion.conceal()) {
					Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
						DesignRow(
							"Fecha",
							MindIcons.Event,
							DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(config.at),
						) {
							val c = Calendar.getInstance().apply { timeInMillis = config.at }
							DatePickerDialog(
								dialogContext,
								{ _, y, m, d ->
									c.set(y, m, d)
									change(config.copy(at = c.timeInMillis))
								},
								c.get(Calendar.YEAR),
								c.get(Calendar.MONTH),
								c.get(Calendar.DAY_OF_MONTH),
							)
								.show()
						}
						DesignRow(
							"Hora",
							MindIcons.Clock,
							DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(config.at),
						) {
							val c = Calendar.getInstance().apply { timeInMillis = config.at }
							TimePickerDialog(
								dialogContext,
								{ _, h, m ->
									c.set(Calendar.HOUR_OF_DAY, h)
									c.set(
										Calendar.MINUTE,
										m,
									)
									c.set(Calendar.SECOND, 0)
									c.set(
										Calendar.MILLISECOND,
										0,
									)
									change(config.copy(at = c.timeInMillis))
								},
								c.get(Calendar.HOUR_OF_DAY),
								c.get(Calendar.MINUTE),
								android.text.format.DateFormat.is24HourFormat(context),
							)
								.show()
						}
						if (!validTime)
							Text(
								"Elige una hora futura.",
								color = MaterialTheme.colorScheme.error,
								style = MaterialTheme.typography.bodySmall,
							)
						if (!ReminderScheduler.exactAllowed(context))
							Text(
								"El aviso puede llegar con retraso. Puedes permitir alarmas exactas en las opciones.",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						TextButton(onClick = { advanced = true }) {
							Icon(
								MindIcons.Tune,
								null,
							)
							Spacer(Modifier.width(8.dp))
							Text("Repetición y opciones de aviso")
						}
					}
				}
				OutlinedTextField(
					message,
					{ value ->
						onEvent(
							EditNoteEvent.EnteredContent(
								doc.copy(
									text = value,
									reminder = if (notify) config.copy(message = value) else null,
								)
									.encode()
							)
						)
					},
					enabled = enabled,
					modifier = Modifier.fillMaxWidth(),
					label = { Text("Detalle (opcional)") },
					shape = RoundedCornerShape(16.dp),
				)
				if (denied) {
					Text(
						"Activa las notificaciones o desmarca Avisarme para guardarlo sin aviso.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.error,
					)
					TextButton(
						onClick = {
							context.startActivity(
								Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
									.putExtra(
										Settings.EXTRA_APP_PACKAGE,
										context.packageName,
									)
							)
						}
					) {
						Text("Permitir notificaciones")
					}
				}
				if (state.saveStatus == SaveStatus.ERROR)
					Text(
						"No se pudo guardar. Vuelve a intentarlo.",
						color = MaterialTheme.colorScheme.error,
					)
			}
		}
	}
}
