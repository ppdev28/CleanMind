package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.util.DateUtils

@Composable
fun HomeNoteItem(
	notes: List<Note>,
	modifier: Modifier = Modifier,
	onClick: (Note) -> Unit,
	onLongClick: (Note) -> Unit = {},
	showContent: Boolean = true,
) {
	if (notes.isEmpty()) return
	val timestamp = notes.first().timestamp
	val date =
		when {
			DateUtils.isToday(timestamp) -> stringResource(R.string.today)
			DateUtils.isYesterday(timestamp) -> stringResource(R.string.yesterday)
			else -> DateUtils.shortDate(timestamp)
		}
	Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text(
			date,
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
		)
		notes.forEach { note ->
			SimpleNoteItem(
				note = note,
				onClick = { onClick(note) },
				onDelete = { onLongClick(note) },
				showContent = showContent,
			)
		}
	}
}
