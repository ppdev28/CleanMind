package com.example.cleannotes.presentation.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.util.DateUtils

@Composable
fun DesignIcon(
	asset: ImageVector,
	description: String? = null,
	modifier: Modifier = Modifier,
	tint: Color = MaterialTheme.colorScheme.onSurface,
) {
	Icon(asset, description, modifier.size(24.dp), tint = tint)
}

@Composable
fun DesignIconButton(
	asset: ImageVector,
	description: String,
	enabled: Boolean = true,
	onClick: () -> Unit,
) {
	IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
		DesignIcon(
			asset,
			description,
		)
	}
}

@Composable
fun DesignRow(
	title: String,
	icon: ImageVector,
	subtitle: String? = null,
	selected: Boolean = false,
	onClick: () -> Unit,
) {
	Surface(
		onClick = onClick,
		modifier = Modifier.appSurface(RoundedCornerShape(16.dp), selected = selected),
		shape = RoundedCornerShape(16.dp),
		color = Color.Transparent,
	) {
		Row(
			Modifier.fillMaxWidth()
				.heightIn(min = 64.dp)
				.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			DesignIcon(icon)
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(title, style = MaterialTheme.typography.bodyMedium)
				subtitle?.let {
					Text(
						it,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
			DesignIcon(if (selected) MindIcons.Check else MindIcons.Arrow)
		}
	}
}

@Composable
fun DesignNoteCard(
	note: Note,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
	compact: Boolean = false,
	dateTimestamp: Long = note.timestamp,
	selectionMode: Boolean = false,
	selected: Boolean = false,
) {
	val interactions = remember { MutableInteractionSource() }
	val pressed by interactions.collectIsPressedAsState()
	val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "notePress")
	val selectionChecked = selected
	val doc = note.document
	val detail = buildList {
		if (doc.pinned) add("Fijada")
		add(
			if (DateUtils.isToday(dateTimestamp)) "Hoy"
			else if (DateUtils.isYesterday(dateTimestamp)) "Ayer"
			else DateUtils.shortDate(dateTimestamp)
		)
		add("${doc.images.size} ${if (doc.images.size == 1) "imagen" else "imágenes"}")
	}
		.joinToString(" · ")
	Surface(
		modifier =
			modifier.graphicsLayer { scaleX = scale; scaleY = scale }
				.appSurface(RoundedCornerShape(22.dp), selected = selected),
		shape = RoundedCornerShape(22.dp),
		color = Color.Transparent,
	) {
		Column(
			Modifier.fillMaxWidth().heightIn(min = 112.dp)
				// Keep the indication inside Surface, whose rounded shape clips its drawing.
				.combinedClickable(
					interactionSource = interactions, indication = LocalIndication.current,
					onClick = onClick, onLongClick = onLongClick,
					onLongClickLabel = if (selectionMode) "Alternar selección" else "Seleccionar nota"
				)
				.semantics { if (selectionMode) this.selected = selectionChecked }
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					note.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
					maxLines = 3, overflow = TextOverflow.Ellipsis
				)
				if (selectionMode) Checkbox(selected, null, Modifier.size(24.dp))
			}
			if (doc.subtitle.isNotBlank())
				Text(
					doc.subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = if (compact) 3 else 2,
					overflow = TextOverflow.Ellipsis,
				)
			Text(
				detail,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
fun DesignSnackbarHost(state: SnackbarHostState) {
	SnackbarHost(state) { data ->
		Surface(
			onClick = {
				if (data.visuals.actionLabel != null) data.performAction() else data.dismiss()
			},
			modifier = Modifier.padding(horizontal = 24.dp),
			shape = RoundedCornerShape(14.dp),
			color = MaterialTheme.colorScheme.surfaceContainer,
		) {
			Row(
				Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Text(
					data.visuals.message + (data.visuals.actionLabel?.let { " · $it" } ?: ""),
					style = MaterialTheme.typography.bodyMedium,
				)
			}
		}
	}
}
