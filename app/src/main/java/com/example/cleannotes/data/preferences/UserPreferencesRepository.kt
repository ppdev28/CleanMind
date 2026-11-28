package com.example.cleannotes.data.preferences

import com.example.cleannotes.model.ThemeMode
import com.example.cleannotes.model.Appearance
import com.example.cleannotes.model.CustomTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
	val appearance: Flow<Appearance>
	suspend fun selectPalette(id: String)
	suspend fun saveCustomTheme(theme: CustomTheme)
	suspend fun deleteCustomTheme(id: String)

	val themeMode: Flow<ThemeMode>

	suspend fun setThemeMode(mode: ThemeMode)
}
