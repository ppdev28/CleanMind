package com.example.cleannotes.data.samples

import android.content.Context
import android.util.Log
import com.example.cleannotes.data.repository.NoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DevelopmentContentDependencies {
	fun notes(): NoteRepository
}

private val contentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Requested sample content is installed once in development builds, never in release builds. */
fun installDevelopmentContent(context: Context) {
	contentScope.launch {
		try {
			val preferences =
				context.getSharedPreferences("development_content", Context.MODE_PRIVATE)
			val key = "science-short-v1"
			if (preferences.getBoolean(key, false)) return@launch
			val collection =
				context.assets.open("science-notes.json").bufferedReader().use {
					Json.decodeFromString<SampleNoteCollection>(it.readText())
				}
			val repository =
				EntryPointAccessors.fromApplication(
					context,
					DevelopmentContentDependencies::class.java,
				)
					.notes()
			collection.addMissingTo(repository)
			check(preferences.edit().putBoolean(key, true).commit())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.e("DevelopmentContent", "Could not install requested sample notes", e)
		}
	}
}
