package com.example.cleannotes.presentation.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleannotes.data.export.NoteExport
import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.NoteDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val notes: NoteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun copyAll() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val snapshot = notes.observeNotes().first()
                snapshot.forEachIndexed { index, note ->
                    _status.value = "Copiando ${index + 1} de ${snapshot.size}…"
                    NoteExport.saveToFolder(context, note.title, NoteDocument.decode(note.content), false, "nota-${note.id}.txt")
                }
                _status.value = "${snapshot.size} notas copiadas"
                NoteExport.prefs(context).edit().remove("error").apply()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _status.value = "No se completó la copia. Revisa el acceso a la carpeta y vuelve a intentarlo." }
            finally { _busy.value = false }
        }
    }
}
