package com.example.cleannotes.presentation.ui.design

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/** Material 3 tonal decoration for Surface, which owns shape clipping and content drawing. */
@Composable
fun Modifier.appSurface(
	shape: Shape = RoundedCornerShape(24.dp),
	prominent: Boolean = false,
	selected: Boolean = false,
): Modifier {
	val colors = MaterialTheme.colorScheme
	val container = when {
		selected -> colors.tertiaryContainer
		prominent -> colors.surfaceContainer
		else -> colors.surfaceContainerLow
	}
	return background(container, shape).border(
		1.dp,
		if (selected) colors.tertiary else colors.outlineVariant,
		shape,
	)
}

/** The sheet's own Surface paints its navigation inset, keeping the bottom edge continuous. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
	onDismissRequest: () -> Unit,
	sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
	scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f),
	content: @Composable ColumnScope.() -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismissRequest,
		sheetState = sheetState,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		contentColor = MaterialTheme.colorScheme.onSurface,
		scrimColor = scrimColor,
	) {
		val view = LocalView.current
		val window = generateSequence(view.parent) { it.parent }
			.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
		SideEffect {
			if (Build.VERSION.SDK_INT >= 29) window?.isNavigationBarContrastEnforced = false
		}
		content()
	}
}
