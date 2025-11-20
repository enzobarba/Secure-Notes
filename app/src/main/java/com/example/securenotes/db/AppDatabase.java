package com.example.securenotes.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.securenotes.model.Note;
import com.example.securenotes.security.SecurityDbManager;
import net.sqlcipher.database.SupportFactory;

@Database (entities = {Note.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase{

    private static AppDatabase instance;
    public abstract NoteDao noteDao ();

    public static synchronized AppDatabase getInstance (Context context) {

        if (instance == null) {
            Context appContext = context.getApplicationContext();

            final byte[] passphrase;
            try {
                // Ottiene la chiave sicura dal SecurityManager
                passphrase = SecurityDbManager.getDatabasePassphrase(appContext);
            } catch (Exception e) {
                throw new RuntimeException("Impossible to obtain passphrase DB", e);
            }

            // Crea SQLCipher con la chiave
            final SupportFactory factory = new SupportFactory(passphrase);

            //Costruisce Room usando SQLCipher
            instance = Room.databaseBuilder(appContext,
                            AppDatabase.class, "secure_notes_db")
                    // .openHelperFactory(factory) dice a Room
                    // di usare SQLCipher invece del DB standard.
                    .openHelperFactory(factory)
                    .build();
        }
        return instance;
    }

}
