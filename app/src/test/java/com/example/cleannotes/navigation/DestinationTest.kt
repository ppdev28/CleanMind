package com.example.cleannotes.navigation

import com.example.cleannotes.presentation.navigation.Destination
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DestinationTest {
	@Test
	fun destinationsRoundTripWithTypedArguments() {
		val destinations =
			listOf(
				Destination.Notebook,
				Destination.Settings,
				Destination.Editor(noteId = 7),
				Destination.Editor(isReminder = true),
			)
		destinations.forEach { destination ->
			val encoded = Json.encodeToString<Destination>(destination)
			assertEquals(destination, Json.decodeFromString<Destination>(encoded))
		}
	}
}
