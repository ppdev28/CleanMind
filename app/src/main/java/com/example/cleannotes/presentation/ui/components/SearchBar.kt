package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
	val focus = LocalFocusManager.current
	OutlinedTextField(
		value = query,
		onValueChange = onQueryChange,
		singleLine = true,
		placeholder = {
			Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium)
		},
		leadingIcon = { Icon(Icons.Outlined.Search, null) },
		trailingIcon = {
			if (query.isNotEmpty())
				IconButton(onClick = { onQueryChange("") }) {
					Icon(Icons.Default.Close, stringResource(R.string.clear_search))
				}
		},
		shape = RoundedCornerShape(18.dp),
		textStyle = MaterialTheme.typography.bodyLarge,
		colors =
			OutlinedTextFieldDefaults.colors(
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				unfocusedContainerColor = MaterialTheme.colorScheme.surface,
				focusedContainerColor = MaterialTheme.colorScheme.surface,
			),
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
		keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
		modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
	)
}
