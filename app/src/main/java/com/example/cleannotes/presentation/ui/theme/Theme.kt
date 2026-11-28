package com.example.cleannotes.presentation.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.ColorScheme
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import com.example.cleannotes.model.Appearance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

private val LightColors =
	lightColorScheme(
		primary = Gray900,
		onPrimary = Gray0,
		primaryContainer = Gray200,
		onPrimaryContainer = Gray900,
		inversePrimary = AccentBlueLight,
		secondary = Gray600,
		onSecondary = Gray0,
		secondaryContainer = Gray150,
		onSecondaryContainer = Gray900,
		tertiary = AccentBlue,
		onTertiary = Gray0,
		tertiaryContainer = BlueContainer,
		onTertiaryContainer = BlueInk,
		background = Gray100,
		onBackground = Gray900,
		surface = Gray50,
		onSurface = Gray900,
		surfaceVariant = Gray150,
		onSurfaceVariant = Gray600,
		surfaceDim = Gray200,
		surfaceBright = Gray0,
		surfaceContainerLowest = Gray0,
		surfaceContainerLow = Gray50,
		surfaceContainer = Gray150,
		surfaceContainerHigh = Gray150,
		surfaceContainerHighest = Gray200,
		surfaceTint = Color.Transparent,
		inverseSurface = Gray800,
		inverseOnSurface = Gray100,
		outline = Gray500,
		outlineVariant = Gray300,
		error = Color(0xFFB3261E),
		onError = Gray0,
		errorContainer = Color(0xFFFCE8E6),
		onErrorContainer = Color(0xFF601410),
		scrim = Color.Black,
		primaryFixed = BlueContainer,
		primaryFixedDim = AccentBlueLight,
		onPrimaryFixed = BlueInk,
		onPrimaryFixedVariant = BlueInk,
		secondaryFixed = Gray200,
		secondaryFixedDim = Gray300,
		onSecondaryFixed = Gray900,
		onSecondaryFixedVariant = Gray600,
		tertiaryFixed = BlueContainer,
		tertiaryFixedDim = AccentBlueLight,
		onTertiaryFixed = BlueInk,
		onTertiaryFixedVariant = BlueInk,
	)

private val DarkColors =
	darkColorScheme(
		primary = Color(0xFFF2F2F2),
		onPrimary = Color(0xFF101010),
		primaryContainer = Color(0xFF303030),
		onPrimaryContainer = Color(0xFFF2F2F2),
		inversePrimary = AccentBlue,
		secondary = Gray300,
		onSecondary = Gray900,
		secondaryContainer = Gray800,
		onSecondaryContainer = Gray200,
		tertiary = AccentBlueLight,
		onTertiary = BlueInk,
		tertiaryContainer = BlueContainerDark,
		onTertiaryContainer = BlueInkDark,
		background = Color.Black,
		onBackground = Color.White,
		surface = Color(0xFF141414),
		onSurface = Color.White,
		surfaceVariant = Gray800,
		onSurfaceVariant = Gray300,
		surfaceDim = Gray950,
		surfaceBright = Gray750,
		surfaceContainerLowest = Color.Black,
		surfaceContainerLow = Color(0xFF141414),
		surfaceContainer = Gray850,
		surfaceContainerHigh = Gray800,
		surfaceContainerHighest = Gray750,
		surfaceTint = Color.Transparent,
		inverseSurface = Gray200,
		inverseOnSurface = Gray900,
		outline = Gray500,
		outlineVariant = Color(0xFF404040),
		error = Color(0xFFF2B8B5),
		onError = Color(0xFF601410),
		errorContainer = Color(0xFF5C2523),
		onErrorContainer = Color(0xFFFCE8E6),
		scrim = Color.Black,
		primaryFixed = BlueContainer,
		primaryFixedDim = AccentBlueLight,
		onPrimaryFixed = BlueInk,
		onPrimaryFixedVariant = BlueInk,
		secondaryFixed = Gray200,
		secondaryFixedDim = Gray300,
		onSecondaryFixed = Gray900,
		onSecondaryFixedVariant = Gray600,
		tertiaryFixed = BlueContainer,
		tertiaryFixedDim = AccentBlueLight,
		onTertiaryFixed = BlueInk,
		onTertiaryFixedVariant = BlueInk,
	)

internal fun themedColors(dark: Boolean, appearance: Appearance): ColorScheme {
	val base = if (dark) DarkColors else LightColors
	val raw = appearance.selected.accent?.toInt() ?: return base
	val hsl = FloatArray(3); ColorUtils.colorToHSL(raw, hsl)
	var primary = raw
	repeat(51) {
		if (ColorUtils.calculateContrast(primary, base.surface.toArgb()) < 4.5) {
			hsl[2] = (hsl[2] + if (dark) 0.02f else -0.02f).coerceIn(0f, 1f)
			primary = ColorUtils.HSLToColor(hsl)
		}
	}
	fun ink(color: Int): Color = if (ColorUtils.calculateContrast(
			android.graphics.Color.BLACK,
			color
		) >= ColorUtils.calculateContrast(android.graphics.Color.WHITE, color)
	) Color.Black else Color.White

	val container = ColorUtils.blendARGB(base.surface.toArgb(), raw, if (dark) 0.28f else 0.15f)
	return base.copy(
		primary = Color(primary),
		onPrimary = ink(primary),
		primaryContainer = Color(container),
		onPrimaryContainer = ink(container),
		secondary = Color(primary),
		onSecondary = ink(primary),
		secondaryContainer = Color(container),
		onSecondaryContainer = ink(container),
		tertiary = Color(primary),
		onTertiary = ink(primary),
		tertiaryContainer = Color(container),
		onTertiaryContainer = ink(container),
		inversePrimary = Color(primary),
		primaryFixed = Color(container),
		primaryFixedDim = Color(container),
		onPrimaryFixed = ink(container),
		onPrimaryFixedVariant = ink(container),
		secondaryFixed = Color(container),
		secondaryFixedDim = Color(container),
		onSecondaryFixed = ink(container),
		onSecondaryFixedVariant = ink(container),
		tertiaryFixed = Color(container),
		tertiaryFixedDim = Color(container),
		onTertiaryFixed = ink(container),
		onTertiaryFixedVariant = ink(container)
	)
}

@Composable
fun CleanMindTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	appearance: Appearance = Appearance(),
	content: @Composable () -> Unit,
) {
	val view = LocalView.current
	if (!view.isInEditMode) {
		SideEffect {
			val window = (view.context as Activity).window
			if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
			WindowCompat.getInsetsController(window, view).apply {
				isAppearanceLightStatusBars = !darkTheme
				isAppearanceLightNavigationBars = !darkTheme
			}
		}
	}
	MaterialTheme(
		colorScheme = remember(darkTheme, appearance) { themedColors(darkTheme, appearance) },
		typography = Typography,
		content = {
			CompositionLocalProvider(
				LocalContentColor provides MaterialTheme.colorScheme.onBackground
			) {
				Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
			}
		},
	)
}
