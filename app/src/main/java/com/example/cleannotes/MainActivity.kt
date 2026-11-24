package com.example.cleannotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.cleannotes.presentation.navigation.NotesApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	private var notificationNoteId by androidx.compose.runtime.mutableStateOf<Int?>(null)

	override fun onNewIntent(intent: android.content.Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		notificationNoteId = intent.getIntExtra("noteId", -1).takeIf { it > 0 }
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		val splash = installSplashScreen()
		super.onCreate(savedInstanceState)
		var contentReady by androidx.compose.runtime.mutableStateOf(false)
		splash.setKeepOnScreenCondition { !contentReady }
		splash.setOnExitAnimationListener { screen ->
			if (
				android.os.Build.VERSION.SDK_INT >= 26 &&
				!android.animation.ValueAnimator.areAnimatorsEnabled()
			)
				screen.remove()
			else
				screen.view
					.animate()
					.alpha(0f)
					.setDuration(180L)
					.withEndAction { screen.remove() }
					.start()
		}
		enableEdgeToEdge()
		lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
			com.example.cleannotes.data.reminders.ReminderScheduler.restore(applicationContext)
		}
		notificationNoteId = intent.getIntExtra("noteId", -1).takeIf { it > 0 }
		setContent {
			NotesApp(
				notificationNoteId = notificationNoteId,
				onNotificationOpened = { notificationNoteId = null },
				onReady = { contentReady = true },
			)
		}
	}
}
