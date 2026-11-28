package com.example.cleannotes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cleannotes.model.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class DataStoreUserPreferencesRepository
@Inject
constructor(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {
	override val themeMode =
		dataStore.data.map { preferences ->
			ThemeMode.entries.firstOrNull { it.name == preferences[THEME] } ?: ThemeMode.SYSTEM
		}

	override suspend fun setThemeMode(mode: ThemeMode) {
		dataStore.edit { it[THEME] = mode.name }
	}

	private companion object {
		val THEME = stringPreferencesKey("theme")
	}
}
