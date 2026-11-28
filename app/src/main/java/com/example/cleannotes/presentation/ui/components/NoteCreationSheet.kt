package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cleannotes.presentation.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCreationSheet(onCreate: (Boolean) -> Unit, onDismiss: () -> Unit) {
	AppBottomSheet(onDismissRequest = onDismiss) {
		Column(
			Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text("Crear", style = MaterialTheme.typography.titleLarge)
			DesignRow("Nueva nota", MindIcons.EditNote, "Guarda una idea o una lista") { onCreate(false) }
			DesignRow(
				"Nuevo recordatorio",
				MindIcons.Notifications,
				"Programa un aviso"
			) { onCreate(true) }
		}
	}
}
