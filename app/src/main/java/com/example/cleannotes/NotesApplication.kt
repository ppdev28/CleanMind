package com.example.cleannotes

import android.app.Application
import com.example.cleannotes.data.samples.installDevelopmentContent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotesApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		installDevelopmentContent(this)
	}
}
