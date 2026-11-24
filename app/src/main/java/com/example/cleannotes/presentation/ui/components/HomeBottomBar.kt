package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R

@Composable
fun HomeBottomBar(modifier: Modifier = Modifier, onQuickAdd: (String, Boolean) -> Unit) {
	var text by rememberSaveable { mutableStateOf("") }
	var reminder by rememberSaveable { mutableStateOf(false) }
	val focus = LocalFocusManager.current
	val submit = {
		if (text.isNotBlank()) {
			onQuickAdd(text.trim(), reminder)
			text = ""
			focus.clearFocus()
		}
	}
	OutlinedTextField(
		value = text,
		onValueChange = { text = it },
		singleLine = true,
		placeholder = {
			Text(
				stringResource(R.string.quick_note_hint),
				style = MaterialTheme.typography.bodyMedium,
			)
		},
		leadingIcon = {
			IconToggleButton(checked = reminder, onCheckedChange = { reminder = it }) {
				Icon(Icons.Outlined.Notifications, stringResource(R.string.reminder_mode))
			}
		},
		trailingIcon = {
			IconButton(onClick = submit, enabled = text.isNotBlank()) {
				Icon(Icons.Default.Add, stringResource(R.string.create))
			}
		},
		textStyle = MaterialTheme.typography.bodyLarge,
		shape = RoundedCornerShape(20.dp),
		colors =
			OutlinedTextFieldDefaults.colors(
				unfocusedContainerColor = MaterialTheme.colorScheme.surface,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
			),
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
		keyboardActions = KeyboardActions(onDone = { submit() }),
		modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
	)
}
