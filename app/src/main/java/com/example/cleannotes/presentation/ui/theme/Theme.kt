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
import androidx.compose.material3.ColorScheme
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
		primary = AccentBlue,
		onPrimary = Gray0,
		primaryContainer = BlueContainer,
		onPrimaryContainer = BlueInk,
		inversePrimary = AccentBlueLight,
		secondary = Gray600,
		onSecondary = Gray0,
		secondaryContainer = Gray150,
		onSecondaryContainer = Gray900,
		tertiary = AccentBlue,
		onTertiary = Gray0,
		tertiaryContainer = BlueContainer,
		onTertiaryContainer = BlueInk,
		background = Color(0xFFF0F0F0),
		onBackground = Color(0xFF3C3C3C),
		surface = Color(0xFFF6F6F6),
		onSurface = Color(0xFF3C3C3C),
		surfaceVariant = Gray150,
		onSurfaceVariant = Color(0xFF656565),
		surfaceDim = Gray200,
		surfaceBright = Gray0,
		surfaceContainerLowest = Gray0,
		surfaceContainerLow = Color(0xFFF6F6F6),
		surfaceContainer = Gray150,
		surfaceContainerHigh = Gray150,
		surfaceContainerHighest = Gray200,
		surfaceTint = Color.Transparent,
		inverseSurface = Gray800,
		inverseOnSurface = Gray100,
		outline = Gray500,
		outlineVariant = Color(0xFFDADADA),
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
		primary = AccentBlueLight,
		onPrimary = Color(0xFF21313D),
		primaryContainer = Color(0xFF2B3B46),
		onPrimaryContainer = Color(0xFFD4DFE7),
		inversePrimary = AccentBlue,
		secondary = Gray300,
		onSecondary = Gray900,
		secondaryContainer = Gray800,
		onSecondaryContainer = Gray200,
		tertiary = AccentBlueLight,
		onTertiary = BlueInk,
		tertiaryContainer = BlueContainerDark,
		onTertiaryContainer = BlueInkDark,
		background = Color(0xFF181818),
		onBackground = Color(0xFFDCDCDC),
		surface = Color(0xFF202020),
		onSurface = Color(0xFFDCDCDC),
		surfaceVariant = Gray800,
		onSurfaceVariant = Color(0xFFB3B3B3),
		surfaceDim = Gray950,
		surfaceBright = Gray750,
		surfaceContainerLowest = Color(0xFF181818),
		surfaceContainerLow = Color(0xFF202020),
		surfaceContainer = Color(0xFF282828),
		surfaceContainerHigh = Gray800,
		surfaceContainerHighest = Gray750,
		surfaceTint = Color.Transparent,
		inverseSurface = Gray200,
		inverseOnSurface = Gray900,
		outline = Gray500,
		outlineVariant = Color(0xFF363636),
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

internal fun appColors(dark: Boolean): ColorScheme = if (dark) DarkColors else LightColors

@Composable
fun CleanMindTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
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
		colorScheme = appColors(darkTheme),
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
