package com.example.cleannotes.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cleannotes.data.preferences.DataStoreUserPreferencesRepository
import com.example.cleannotes.model.ThemeMode
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class UserPreferencesRepositoryTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun legacyThemeMigratesAndNewSelectionSurvivesReopening() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("theme", "DARK").commit()
        val file = File(directory.root, "test.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, "appearance")),
            scope = scope, produceFile = { file }
        )
        try {
            val repository = DataStoreUserPreferencesRepository(store)
            assertEquals(ThemeMode.DARK, repository.themeMode.first())
            repository.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
        } finally { scope.cancel(); scope.coroutineContext[Job]!!.join() }
        val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
            assertEquals(ThemeMode.LIGHT, DataStoreUserPreferencesRepository(reopened).themeMode.first())
        } finally { reopenedScope.cancel(); reopenedScope.coroutineContext[Job]!!.join() }
    }
}
