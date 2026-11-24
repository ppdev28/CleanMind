package com.example.cleannotes.presentation.ui.design

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Capture only ambient decoration for cards, keeping note text out of their blur input. */
@Composable
fun GlassScene(blurEnabled: Boolean = true, content: @Composable () -> Unit) {
	val colors = MaterialTheme.colorScheme
	val dark = colors.background.luminance() < 0.5f
	val state =
		rememberHazeState(
			blurEnabled =
				blurEnabled &&
						Build.VERSION.SDK_INT >= 32 &&
						LocalView.current.isHardwareAccelerated &&
						!LocalInspectionMode.current
		)
	CompositionLocalProvider(LocalGlassState provides state) {
		Box(Modifier.fillMaxSize()) {
			Canvas(Modifier.fillMaxSize().hazeSource(state, key = "ambient")) {
				drawRect(colors.background)
				drawRect(
					Brush.radialGradient(
						listOf(
							colors.tertiary.copy(alpha = if (dark) 0.24f else 0.17f),
							Color.Transparent,
						),
						center = Offset(size.width * 0.08f, size.height * 0.16f),
						radius = size.width * 1.2f,
					)
				)
				drawRect(
					Brush.radialGradient(
						listOf(
							colors.onSurface.copy(alpha = if (dark) 0.07f else 0.05f),
							Color.Transparent,
						),
						center = Offset(size.width * 0.95f, size.height * 0.7f),
						radius = size.width,
					)
				)
			}
			content()
		}
	}
}

/**
 * Only the notebook's floating bars sample scrolling content; surfaces sample the ambient layer.
 */
@Composable
fun Modifier.glassContentSource(): Modifier =
	LocalGlassState.current?.let { hazeSource(it, zIndex = 1f, key = "notebook") } ?: this

@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glassMaterial(
	shape: Shape = RoundedCornerShape(24.dp),
	prominent: Boolean = false,
	selected: Boolean = false,
	includeScrollingContent: Boolean = false,
): Modifier {
	val colors = MaterialTheme.colorScheme
	val dark = colors.background.luminance() < 0.5f
	val tint = colors.surface.copy(alpha = if (prominent) 0.80f else if (dark) 0.62f else 0.66f)
	val state = LocalGlassState.current
	val style =
		HazeStyle(
			backgroundColor = colors.background,
			tint = HazeTint(tint),
			blurRadius = if (prominent) 28.dp else 20.dp,
			noiseFactor = 0.025f,
			fallbackTint = HazeTint(colors.surface.copy(alpha = 0.94f)),
		)
	val frost =
		if (state != null)
			Modifier.hazeEffect(state, style) {
				inputScale = HazeInputScale.Auto
				canDrawArea = {
					it.key == "ambient" || (includeScrollingContent && it.key == "notebook")
				}
			}
		else Modifier.background(tint)
	return this.shadow(
		if (prominent) 14.dp else 6.dp,
		shape,
		ambientColor = Color.Black.copy(alpha = 0.10f),
		spotColor = Color.Black.copy(alpha = 0.10f),
	)
		.clip(shape)
		.then(frost)
		.background(
			Brush.linearGradient(
				listOf(
					Color.White.copy(alpha = if (dark) 0.065f else 0.32f),
					colors.tertiary.copy(alpha = if (selected) 0.15f else 0.025f),
				)
			)
		)
		.border(
			1.dp,
			Brush.linearGradient(
				listOf(
					if (selected) colors.tertiary.copy(alpha = 0.75f)
					else Color.White.copy(alpha = if (dark) 0.23f else 0.95f),
					colors.outlineVariant.copy(alpha = 0.25f),
				)
			),
			shape,
		)
}

/** The material belongs inside the draggable sheet, so clipping follows its actual position. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
	onDismissRequest: () -> Unit,
	sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
	scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f),
	content: @Composable ColumnScope.() -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismissRequest,
		sheetState = sheetState,
		containerColor = Color.Transparent,
		contentColor = MaterialTheme.colorScheme.onSurface,
		scrimColor = scrimColor,
		dragHandle = null,
	) {
		Column(
			Modifier.fillMaxWidth()
				.glassMaterial(
					RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
					prominent = true,
				)
		) {
			BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
			content()
		}
	}
}
