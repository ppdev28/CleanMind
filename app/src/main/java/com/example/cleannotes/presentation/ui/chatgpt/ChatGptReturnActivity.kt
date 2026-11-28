package com.example.cleannotes.presentation.ui.chatgpt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.cleannotes.MainActivity

/**
 * This link only brings the existing task forward. OAuth is validated independently over loopback.
 */
class ChatGptReturnActivity : Activity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		startActivity(
			Intent(this, MainActivity::class.java)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
		)
		finish()
	}
}
