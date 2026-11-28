package com.example.cleannotes.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/** Removes retired color choices while preserving the light/dark mode and other preferences. */
internal object RetiredThemePreferencesMigration : DataMigration<Preferences> {
	private val obsoleteKeys = listOf("palette", "custom_palettes").map(::stringPreferencesKey)

	override suspend fun shouldMigrate(currentData: Preferences) =
		obsoleteKeys.any { currentData.contains(it) }

	override suspend fun migrate(currentData: Preferences): Preferences =
		currentData.toMutablePreferences().apply { obsoleteKeys.forEach { remove(it) } }

	override suspend fun cleanUp() = Unit
}
