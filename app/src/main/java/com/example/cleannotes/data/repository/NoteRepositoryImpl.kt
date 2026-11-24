package com.example.cleannotes.data.repository

import com.example.cleannotes.data.source.NoteDao
import com.example.cleannotes.data.source.toNoteEntity
import com.example.cleannotes.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(private val dao: NoteDao, @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> = dao.observeNotes().map { entities -> entities.map { it.toNote() } }
    override suspend fun getNote(id: Int): Note? = dao.getNoteById(id)?.toNote()
    override suspend fun saveNote(note: Note): Int {
        require(note.title.isNotBlank()) { "A note must have a title" }
        val previous = note.id?.let { dao.getNoteById(it)?.toNote() }
        val previousDocument = previous?.let { com.example.cleannotes.model.NoteDocument.decode(it.content) }
        val document = com.example.cleannotes.model.NoteDocument.decode(note.content).copy(
            createdAt = previousDocument?.createdAt ?: previous?.timestamp ?: note.timestamp
        )
        val id = dao.insertNote(note.copy(content = document.encode()).toNoteEntity()).toInt()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.cleannotes.data.reminders.ReminderScheduler.update(context, id, note.title, if (!document.reminderCompleted) document.reminder else null)
            if (com.example.cleannotes.data.export.NoteExport.folder(context) != null) {
                try {
                    com.example.cleannotes.data.export.NoteExport.saveToFolder(context, note.title, document, false, "nota-$id.txt")
                    com.example.cleannotes.data.export.NoteExport.prefs(context).edit().remove("error").apply()
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { com.example.cleannotes.data.export.NoteExport.prefs(context).edit().putString("error", "No se pudo actualizar la copia de la carpeta. Revisa el acceso en Ajustes.").apply() }
            }
        }
        return id
    }
    override suspend fun deleteNote(note: Note) {
        dao.deleteNote(note.toNoteEntity())
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            note.id?.let { com.example.cleannotes.data.reminders.ReminderScheduler.cancel(context, it) }
        }
    }
}
