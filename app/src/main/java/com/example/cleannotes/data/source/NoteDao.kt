package com.example.cleannotes.data.source

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY timestamp DESC, id DESC")
    fun observeNotes(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long
    @Delete
    suspend fun deleteNote(note: NoteEntity): Int
}
