package com.example.cleannotes.presentation

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.cleannotes.presentation.ui.theme.appColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppColorsTest {
	@Test
	fun steelBlueKeepsTextAndActionsReadableInBothAppearances() {
		for (dark in listOf(false, true)) {
			val colors = appColors(dark)
			assertEquals(if (dark) 0xFFA1BDD2.toInt() else 0xFF3E7194.toInt(), colors.primary.toArgb())
			listOf(
				colors.onSurface to colors.surface, colors.onBackground to colors.background,
				colors.onSurfaceVariant to colors.surfaceContainerHighest,
				colors.primary to colors.surface, colors.onPrimary to colors.primary,
				colors.onPrimaryContainer to colors.primaryContainer,
			).forEach { (text, background) ->
				assertTrue(
					"dark=$dark: text contrast",
					ColorUtils.calculateContrast(text.toArgb(), background.toArgb()) >= 4.5
				)
			}
		}
	}
}
