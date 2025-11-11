package com.example.securenotes.db;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.securenotes.model.Note;
import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert (Note note);

    @Update
    void update (Note note);

    @Delete
    void delete (Note note);

    @Query ("SELECT * FROM notes ORDER BY timestamp DESC")
    LiveData<List<Note>> getAllNotes();
}
