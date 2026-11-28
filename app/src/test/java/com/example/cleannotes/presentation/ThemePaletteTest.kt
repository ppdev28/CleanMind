package com.example.cleannotes.presentation

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.theme.themedColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ThemePaletteTest {
	@Test
	fun softerPalettesKeepTextAndActionsReadable() {
		for (dark in listOf(false, true)) for (preset in Appearance.presets) {
			val colors = themedColors(dark, Appearance(preset.id))
			listOf(
				colors.onSurface to colors.surface, colors.onBackground to colors.background,
				colors.onSurfaceVariant to colors.surfaceContainerHighest,
				colors.primary to colors.surface, colors.onPrimary to colors.primary
			).forEach { (text, background) ->
				assertTrue(
					"${preset.id}, dark=$dark: text contrast",
					ColorUtils.calculateContrast(text.toArgb(), background.toArgb()) >= 4.5
				)
			}
		}
	}

	@Test
	fun grayPaletteUsesNeutralRolesInBothAppearances() {
		for (dark in listOf(false, true)) {
			val colors = themedColors(dark, Appearance("gray"))
			listOf(
				colors.primary, colors.onPrimary, colors.primaryContainer, colors.secondary,
				colors.tertiary, colors.tertiaryContainer, colors.background, colors.surface,
				colors.inversePrimary, colors.primaryFixed, colors.tertiaryFixed
			).forEach {
				assertEquals(it.red, it.green, 0.001f)
				assertEquals(it.green, it.blue, 0.001f)
			}
			assertTrue(
				ColorUtils.calculateContrast(
					colors.primary.toArgb(),
					colors.surface.toArgb()
				) >= 4.5
			)
			assertTrue(
				ColorUtils.calculateContrast(
					colors.onPrimary.toArgb(),
					colors.primary.toArgb()
				) >= 4.5
			)
		}
		assertNotEquals(
			themedColors(true, Appearance()).background,
			themedColors(true, Appearance("gray")).background
		)
	}

	@Test
	fun customAccentsRemainReadableInBothAppearances() {
		for (dark in listOf(false, true)) for (seed in listOf(
			0xFFFFFFFFL,
			0xFF000000L,
			0xFFFFFF00L,
			0xFF3E7194L
		)) {
			val theme = CustomTheme("test", "Personal", seed)
			val colors = themedColors(dark, Appearance(theme.id, listOf(theme)))
			assertTrue(
				ColorUtils.calculateContrast(
					colors.primary.toArgb(),
					colors.surface.toArgb()
				) >= 4.5
			)
			assertTrue(
				ColorUtils.calculateContrast(
					colors.onPrimary.toArgb(),
					colors.primary.toArgb()
				) >= 4.5
			)
			assertTrue(
				ColorUtils.calculateContrast(
					colors.onPrimaryContainer.toArgb(),
					colors.primaryContainer.toArgb()
				) >= 4.5
			)
		}
	}
}
