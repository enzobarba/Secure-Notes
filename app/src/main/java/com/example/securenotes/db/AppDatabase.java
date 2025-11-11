package com.example.securenotes.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.securenotes.model.Note;

@Database (entities = {Note.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase{

    public abstract NoteDao noteDao ();
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance (Context context) {

        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "secure_notes_db")
                    .build();
        }
        return instance;

    }

}
