package com.example.cleannotes.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.cleannotes.data.preferences.DataStoreUserPreferencesRepository
import com.example.cleannotes.data.preferences.UserPreferencesRepository
import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.data.repository.NoteRepositoryImpl
import com.example.cleannotes.data.source.NoteDao
import com.example.cleannotes.data.source.NoteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userPreferences by
preferencesDataStore(
	name = "user_preferences",
	produceMigrations = { context ->
		listOf(SharedPreferencesMigration(context, "appearance"))
	},
)

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
	@Provides
	@Singleton
	fun database(@ApplicationContext context: Context): NoteDatabase =
		Room.databaseBuilder(context, NoteDatabase::class.java, NoteDatabase.DATABASE_NAME)
			.addMigrations(NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4)
			.build()

	@Provides
	fun noteDao(database: NoteDatabase): NoteDao = database.noteDao

	@Provides
	@Singleton
	fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
		context.userPreferences
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
	@Binds
	@Singleton
	abstract fun notes(implementation: NoteRepositoryImpl): NoteRepository

	@Binds
	@Singleton
	abstract fun preferences(
		implementation: DataStoreUserPreferencesRepository
	): UserPreferencesRepository
}
