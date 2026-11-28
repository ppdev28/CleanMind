package com.example.cleannotes.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomTheme(val id: String, val name: String, val accent: Long) {
	fun validated(): CustomTheme {
		require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")))
		require(name.trim().isNotEmpty() && name.trim().length <= 30)
		require(accent in 0xFF000000L..0xFFFFFFFFL)
		return copy(name = name.trim())
	}
}

data class ThemePalette(val id: String, val name: String, val accent: Long? = null)

data class Appearance(
	val selectedId: String = "default",
	val customThemes: List<CustomTheme> = emptyList(),
) {
	val selected: ThemePalette
		get() = presets.firstOrNull { it.id == selectedId }
			?: customThemes.firstOrNull { it.id == selectedId }
				?.let { ThemePalette(it.id, it.name, it.accent) }
			?: presets.first()

	companion object {
		val presets = listOf(
			ThemePalette("default", "Original"), ThemePalette("gray", "Gris", 0xFF606060), ThemePalette("steel", "Azul acero", 0xFF3E7194),
			ThemePalette("forest", "Bosque", 0xFF36705A), ThemePalette("lavender", "Lavanda", 0xFF76568E)
		)
	}
}
