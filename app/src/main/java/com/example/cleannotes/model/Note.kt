package com.example.cleannotes.model

/** A note's type is independent of the text the user writes. */
data class Note(
    val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isReminder: Boolean = false
)
