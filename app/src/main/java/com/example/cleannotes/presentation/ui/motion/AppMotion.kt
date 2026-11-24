package com.example.cleannotes.presentation.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * One quiet rhythm for navigation, sheets and content changes. Compose honors system duration
 * scale.
 */
object AppMotion {
	const val Short = 160
	const val Standard = 240

	fun navigation(forward: Boolean = true, direction: Int = 1): ContentTransform {
		val sign = (if (forward) 1 else -1) * direction
		return (fadeIn(tween(Standard)) +
				slideInHorizontally(tween(Standard, easing = FastOutSlowInEasing)) {
					it / 12 * sign
				}) togetherWith
				(fadeOut(tween(Short)) +
						slideOutHorizontally(tween(Standard, easing = FastOutSlowInEasing)) {
							-it / 24 * sign
						})
	}

	fun content(forward: Boolean = true): ContentTransform {
		val sign = if (forward) 1 else -1
		return (fadeIn(tween(180, delayMillis = 60)) +
				slideInHorizontally(tween(Standard, easing = FastOutSlowInEasing)) {
					it / 24 * sign
				}) togetherWith fadeOut(tween(Short))
	}

	fun reveal() =
		fadeIn(tween(Short)) + expandVertically(tween(Standard, easing = FastOutSlowInEasing))

	fun conceal() =
		fadeOut(tween(Short)) + shrinkVertically(tween(Standard, easing = FastOutSlowInEasing))
}
