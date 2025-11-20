package com.example.securenotes.repository;

import com.example.securenotes.db.AppDatabase;
import com.example.securenotes.db.NoteDao;
import com.example.securenotes.model.Note;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.lifecycle.LiveData;
import android.app.Application;

public class NoteRepository {

    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    // thread separato per le operazioni lente (DB)
    private final ExecutorService executorService;

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotes();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    //operazioni in background
    public void insert(Note note) {
        executorService.execute(() -> noteDao.insert(note));
    }

    public void update(Note note) {
        executorService.execute(() -> noteDao.update(note));
    }

    public void delete(Note note) {
        executorService.execute(() -> noteDao.delete(note));
    }
}
