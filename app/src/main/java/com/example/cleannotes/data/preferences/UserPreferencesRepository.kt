package com.example.cleannotes.data.preferences

import com.example.cleannotes.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
