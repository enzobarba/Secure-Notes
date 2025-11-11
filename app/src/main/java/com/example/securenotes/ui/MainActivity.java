package com.example.securenotes.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securenotes.R;

// Importa il fragment della lista (lo creeremo nel prossimo passo)
import com.example.securenotes.ui.list.NoteListFragment;


// "contenitore" per i Fragment.

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // carica il fragment solo al primo avvio, non dopo una rotazione.
        if (savedInstanceState == null) {

            // Avvia una transazione per aggiungere il nostro
            // fragment iniziale (la lista di note).
            getSupportFragmentManager().beginTransaction()
                    // Aggiunge il fragment dentro il contenitore
                    .add(R.id.fragment_container, new NoteListFragment())
                    .commit(); // Applica la modifica
        }
    }
}