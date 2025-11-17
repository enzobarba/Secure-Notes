package com.example.securenotes.ui.dashboard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.securenotes.R;
import com.example.securenotes.databinding.ActivityMainBinding;
import com.example.securenotes.ui.note.NoteDetailFragment;
import com.example.securenotes.ui.note.NoteListFragment;
import com.example.securenotes.ui.archive.FileArchiveFragment;
import android.view.WindowManager;

/*
gestisce la BottomNavigationView
e implementa il 'listener' di NoteListFragment per
navigare ai dettagli.
*/
public class MainActivity extends AppCompatActivity implements NoteListFragment.NoteNavigationListener {

    private ActivityMainBinding binding;

    // Istanzia i fragment "radice" per riutilizzarli
    private final NoteListFragment noteListFragment = new NoteListFragment();

    private final FileArchiveFragment fileArchiveFragment = new FileArchiveFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Impedisce screenshot e oscura l'anteprima nelle App Recenti
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        // Usa ViewBinding per il layout 'activity_main.xml'
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Carica il fragment iniziale (Note) solo al primo avvio
        if (savedInstanceState == null) {
            loadFragment(noteListFragment);
        }

        // Listener per il menu in basso (BottomNavigationView)
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Controlla quale pulsante è stato premuto
            if (itemId == R.id.menu_notes) {
                selectedFragment = noteListFragment;
            } else if (itemId == R.id.menu_files) {
                selectedFragment = fileArchiveFragment; // ERRORE
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment); // Carica il fragment scelto
                return true;
            }
            return false;
        });
    }

    // Metodo helper per caricare i fragment principali (Note/File)
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    @Override
    public void navigateToDetail(int noteId, String title, String content, int color, boolean isPinned) {
        NoteDetailFragment detailFragment = new NoteDetailFragment();

        // Prepara i dati da passare al NoteDetailFragment
        Bundle args = new Bundle();
        args.putInt("NOTE_ID_KEY", noteId);
        args.putString("NOTE_TITLE_KEY", title);
        args.putString("NOTE_CONTENT_KEY", content);
        args.putInt("NOTE_COLOR_KEY", color);
        args.putBoolean("NOTE_PINNED_KEY", isPinned);
        detailFragment.setArguments(args);

        // Esegue la transazione
        getSupportFragmentManager().beginTransaction()
                // Usa il contenitore 'main_fragment_container'
                .replace(R.id.main_fragment_container, detailFragment)
                // Aggiungi alla "cronologia" per il tasto "Indietro"
                .addToBackStack(null)
                .commit();
    }
}