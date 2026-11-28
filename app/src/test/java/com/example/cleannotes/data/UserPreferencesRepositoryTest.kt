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
	@get:Rule
	val directory = TemporaryFolder()

	@Test
	fun customPalettesSurviveReopeningAndDeletingTheActiveOneResetsSelection() = runBlocking {
		val file = File(directory.root, "palettes.preferences_pb")
		var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		var store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
		val theme = com.example.cleannotes.model.CustomTheme("custom-1", "Mi azul", 0xFF3E7194)
		DataStoreUserPreferencesRepository(store).saveCustomTheme(theme)
		scope.cancel(); scope.coroutineContext[Job]!!.join()
		scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		try {
			store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
			val repo = DataStoreUserPreferencesRepository(store)
			assertEquals(theme, repo.appearance.first().customThemes.single())
			assertEquals(theme.id, repo.appearance.first().selectedId)
			repo.deleteCustomTheme(theme.id)
			assertEquals("default", repo.appearance.first().selectedId)
			assertTrue(repo.appearance.first().customThemes.isEmpty())
		} finally {
			scope.cancel(); scope.coroutineContext[Job]!!.join()
		}
	}

	@Test
	fun legacyThemeMigratesAndNewSelectionSurvivesReopening() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		context
			.getSharedPreferences("appearance", Context.MODE_PRIVATE)
			.edit()
			.putString("theme", "DARK")
			.commit()
		val file = File(directory.root, "test.preferences_pb")
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		val store =
			PreferenceDataStoreFactory.create(
				migrations = listOf(SharedPreferencesMigration(context, "appearance")),
				scope = scope,
				produceFile = { file },
			)
		try {
			val repository = DataStoreUserPreferencesRepository(store)
			assertEquals(ThemeMode.DARK, repository.themeMode.first())
			repository.setThemeMode(ThemeMode.LIGHT)
			assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
		} finally {
			scope.cancel()
			scope.coroutineContext[Job]!!.join()
		}
		val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		try {
			val reopened =
				PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
			assertEquals(
				ThemeMode.LIGHT,
				DataStoreUserPreferencesRepository(reopened).themeMode.first(),
			)
		} finally {
			reopenedScope.cancel()
			reopenedScope.coroutineContext[Job]!!.join()
		}
	}
}
