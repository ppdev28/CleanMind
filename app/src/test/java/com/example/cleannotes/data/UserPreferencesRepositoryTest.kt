package com.example.cleannotes.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
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
	fun upgradingRemovesRetiredColorsAndPreservesAppearanceAndOtherPreferences() = runBlocking {
		val file = File(directory.root, "upgrade.preferences_pb")
		val oldScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		val old = PreferenceDataStoreFactory.create(scope = oldScope, produceFile = { file })
		val mode = androidx.datastore.preferences.core.stringPreferencesKey("theme")
		val palette = androidx.datastore.preferences.core.stringPreferencesKey("palette")
		val custom = androidx.datastore.preferences.core.stringPreferencesKey("custom_palettes")
		val other = androidx.datastore.preferences.core.stringPreferencesKey("other")
		try {
			old.edit {
				it[mode] = ThemeMode.DARK.name
				it[palette] = "custom-1"
				it[custom] = "old or malformed theme data"
				it[other] = "preserved"
			}
		} finally {
			oldScope.cancel(); oldScope.coroutineContext[Job]!!.join()
		}
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		try {
			val store = PreferenceDataStoreFactory.create(
				migrations = listOf(com.example.cleannotes.data.preferences.RetiredThemePreferencesMigration),
				scope = scope, produceFile = { file }
			)
			val repository = DataStoreUserPreferencesRepository(store)
			assertEquals(ThemeMode.DARK, repository.themeMode.first())
			val current = store.data.first()
			assertFalse(current.contains(palette)); assertFalse(current.contains(custom))
			assertEquals("preserved", current[other])
			assertFalse(
				com.example.cleannotes.data.preferences.RetiredThemePreferencesMigration.shouldMigrate(
					current
				)
			)
			repository.setThemeMode(ThemeMode.LIGHT)
			assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
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
