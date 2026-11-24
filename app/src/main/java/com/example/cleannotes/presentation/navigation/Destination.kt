package com.example.cleannotes.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Only identifiers are navigation arguments. Note contents stay in the repository/draft. */
@Serializable
sealed interface Destination : NavKey {
	@Serializable
	data object Notebook : Destination

	@Serializable
	data object Settings : Destination

	@Serializable
	data class Editor(
		val noteId: Int? = null,
		val isReminder: Boolean = false,
		val quickCapture: Boolean = false,
		val folderId: Int? = null,
	) : Destination
}
