package com.example.cleannotes

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cleannotes.presentation.navigation.NotesApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationNoteId by androidx.compose.runtime.mutableStateOf<Int?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNoteId = intent.getIntExtra("noteId", -1).takeIf { it > 0 }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.cleannotes.data.reminders.ReminderScheduler.restore(applicationContext)
        }
        notificationNoteId = intent.getIntExtra("noteId", -1).takeIf { it > 0 }
        setContent { NotesApp(notificationNoteId = notificationNoteId, onNotificationOpened = { notificationNoteId = null }) }
    }
}
