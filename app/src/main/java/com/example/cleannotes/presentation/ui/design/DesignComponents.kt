package com.example.cleannotes.presentation.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.util.DateUtils

@Composable
fun DesignIcon(@DrawableRes asset: Int, description: String? = null, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Icon(painterResource(asset), description, modifier.size(24.dp), tint = tint)
}

@Composable
fun DesignIconButton(@DrawableRes asset: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) { DesignIcon(asset, description) }
}

@Composable
fun DesignHeader(title: String, onSettings: () -> Unit, date: Boolean = true) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val dateText = java.text.SimpleDateFormat(if (locale.language == "es") "EEEE, d 'de' MMMM" else "EEEE, d MMMM", locale).format(java.util.Date())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (date) Text(dateText.replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DesignIconButton(R.drawable.figma_settings, "Ajustes", onClick = onSettings)
    }
}

@Composable
fun DesignRow(title: String, @DrawableRes icon: Int, subtitle: String? = null, selected: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (selected) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            DesignIcon(if (selected) R.drawable.figma_check else R.drawable.figma_arrow)
        }
    }
}

@Composable
fun DesignNoteCard(note: Note, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit, compact: Boolean = false) {
    val doc = NoteDocument.decode(note.content)
    val detail = buildList {
        if (doc.pinned) add("Fijada")
        add(if (DateUtils.isToday(note.timestamp)) "Hoy" else if (DateUtils.isYesterday(note.timestamp)) "Ayer" else DateUtils.shortDate(note.timestamp))
        add("${doc.images.size} ${if (doc.images.size == 1) "imagen" else "imágenes"}")
        if (Regex("(?m)^(• |☐ |☑ |\\d+\\. )").containsMatchIn(doc.text)) add("Lista")
    }.joinToString(" · ")
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 112.dp).then(Modifier.noteActions(onClick, onLongClick)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (doc.subtitle.isNotBlank()) Text(doc.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (compact) 3 else 2, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.noteActions(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = this.then(
    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Opciones de la nota")
)

@Composable
fun DesignSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        Surface(onClick = { if (data.visuals.actionLabel != null) data.performAction() else data.dismiss() },
            modifier = Modifier.padding(horizontal = 24.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.visuals.message.contains("archiv", true) || data.visuals.message.contains("restaur", true)) DesignIcon(R.drawable.figma_archive)
                Text(data.visuals.message + (data.visuals.actionLabel?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
