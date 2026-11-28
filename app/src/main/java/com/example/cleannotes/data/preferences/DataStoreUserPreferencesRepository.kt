package com.example.cleannotes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cleannotes.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class DataStoreUserPreferencesRepository
@Inject
constructor(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {
	override val themeMode =
		dataStore.data.map { preferences ->
			ThemeMode.entries.firstOrNull { it.name == preferences[THEME] } ?: ThemeMode.SYSTEM
		}

	private fun custom(preferences: Preferences): List<CustomTheme> = runCatching {
		Json.decodeFromString<List<CustomTheme>>(preferences[CUSTOM] ?: "[]").map { it.validated() }
	}.getOrDefault(emptyList())

	override val appearance = dataStore.data.map { Appearance(it[PALETTE] ?: "default", custom(it)) }
	override suspend fun selectPalette(id: String) {
		dataStore.edit { prefs ->
			require(Appearance.presets.any { it.id == id } || custom(prefs).any { it.id == id })
			prefs[PALETTE] = id
		}
	}

	override suspend fun saveCustomTheme(theme: CustomTheme) {
		val value = theme.validated()
		require(Appearance.presets.none { it.id == value.id })
		dataStore.edit { prefs ->
			prefs[CUSTOM] = Json.encodeToString(custom(prefs).filterNot { it.id == value.id } + value)
			prefs[PALETTE] = value.id
		}
	}

	override suspend fun deleteCustomTheme(id: String) {
		dataStore.edit { prefs ->
			prefs[CUSTOM] = Json.encodeToString(custom(prefs).filterNot { it.id == id })
			if (prefs[PALETTE] == id) prefs[PALETTE] = "default"
		}
	}

	override suspend fun setThemeMode(mode: ThemeMode) {
		dataStore.edit { it[THEME] = mode.name }
	}

	private companion object {
		val THEME = stringPreferencesKey("theme")
		val PALETTE = stringPreferencesKey("palette")
		val CUSTOM = stringPreferencesKey("custom_palettes")
	}
}
