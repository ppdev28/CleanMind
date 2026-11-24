package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.edit_note.ExportMenu
import com.example.cleannotes.presentation.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleNoteItem(
	note: Note,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onDelete: () -> Unit = {},
	compact: Boolean = false,
	showContent: Boolean = true,
) {
	var showOptions by rememberSaveable(note.id) { mutableStateOf(false) }
	var showShare by rememberSaveable(note.id) { mutableStateOf(false) }
	val document = remember(note.content) { NoteDocument.decode(note.content) }
	val accent = MaterialTheme.colorScheme.primary
	Surface(
		modifier =
			modifier
				.fillMaxWidth()
				.combinedClickable(
					onClick = onClick,
					onLongClick = { showOptions = true },
					onLongClickLabel = stringResource(R.string.note_options),
				),
		shape = RoundedCornerShape(22.dp),
		color = MaterialTheme.colorScheme.surface,
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
	) {
		Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
			if (showContent)
				Row(verticalAlignment = Alignment.CenterVertically) {
					Surface(
						color = MaterialTheme.colorScheme.surfaceContainerHigh,
						shape = RoundedCornerShape(10.dp),
					) {
						Icon(
							if (note.isReminder) Icons.Outlined.Notifications
							else Icons.AutoMirrored.Outlined.StickyNote2,
							null,
							tint = accent,
							modifier = Modifier.padding(8.dp).size(18.dp),
						)
					}
					Spacer(Modifier.weight(1f))
					IconButton(onClick = { showOptions = true }) {
						Icon(
							Icons.Outlined.MoreHoriz,
							stringResource(R.string.note_options),
							tint = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			Text(
				note.title.ifBlank {
					stringResource(
						if (note.isReminder) R.string.untitled_reminder else R.string.untitled_note
					)
				},
				style = MaterialTheme.typography.titleMedium,
				maxLines = if (compact) 3 else 2,
				overflow = TextOverflow.Ellipsis,
			)
			if (document.subtitle.isNotBlank())
				Text(
					document.subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)
			if (showContent && !note.isReminder)
				Text(
					document.text.ifBlank { stringResource(R.string.empty_content) },
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = if (compact) 5 else 2,
					overflow = TextOverflow.Ellipsis,
				)
			if (!showContent || document.images.isNotEmpty())
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp),
				) {
					Icon(
						Icons.Outlined.Image,
						null,
						Modifier.size(15.dp),
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Text(
						"${document.images.size} " +
								if (document.images.size == 1) "imagen" else "imágenes",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Text(
					DateUtils.shortDate(note.timestamp),
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.weight(1f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				Text(
					DateUtils.getTime(note.timestamp),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
	if (showOptions)
		ModalBottomSheet(
			onDismissRequest = { showOptions = false },
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
			containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		) {
			Column(Modifier.padding(bottom = 24.dp)) {
				Text(
					note.title,
					Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)
				ListItem(
					headlineContent = { Text(stringResource(R.string.share_note)) },
					supportingContent = { Text("Texto o PDF") },
					leadingContent = { Icon(Icons.Outlined.Share, null, tint = accent) },
					colors =
						ListItemDefaults.colors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerLow
						),
					modifier =
						Modifier.clickable {
							showOptions = false
							showShare = true
						}
							.padding(horizontal = 8.dp),
				)
				ListItem(
					headlineContent = {
						Text(
							stringResource(
								if (note.isReminder) R.string.delete_reminder
								else R.string.delete_note
							),
							color = MaterialTheme.colorScheme.error,
						)
					},
					leadingContent = {
						Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
					},
					colors =
						ListItemDefaults.colors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerLow
						),
					modifier =
						Modifier.clickable {
							showOptions = false
							onDelete()
						}
							.padding(horizontal = 8.dp),
				)
			}
		}
	if (showShare) ExportMenu(note.title, document) { showShare = false }
}
