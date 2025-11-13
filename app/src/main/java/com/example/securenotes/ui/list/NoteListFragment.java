package com.example.securenotes.ui.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.MenuItem;
import androidx.appcompat.widget.PopupMenu;

import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentNoteListBinding;
import com.example.securenotes.model.Note;
import com.example.securenotes.ui.NoteAdapter;
import com.example.securenotes.ui.detail.NoteDetailFragment;
import com.example.securenotes.viewmodel.NoteViewModel;

//Fragment che mostra la lista di note (la Dashboard)
public class NoteListFragment extends Fragment {

    private FragmentNoteListBinding binding; // Riferimento sicuro alle View
    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;

    // "Gonfia" il layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNoteListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Collega la logica dopo che la vista è stata creata
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Imposta Adapter e RecyclerView
        noteAdapter = new NoteAdapter();
        binding.recyclerViewNotes.setAdapter(noteAdapter);

        // Collega il ViewModel
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // 3. Osserva i dati LiveData dal ViewModel
        //    eseguito automaticamente quando i dati cambiano.
        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            // Passa la nuova lista all'adapter per aggiornare la UI
            noteAdapter.submitList(notes);
        });

        // 4. Imposta i Click Listener

        // Pulsante "+" per una nuova nota
        binding.fabAddNote.setOnClickListener(v -> {
            navigateToDetail(null); // 'null' = nuova nota
        });

        // Click su una nota esistente
        noteAdapter.setOnItemClickListener(note -> {
            navigateToDetail(note); // Passa la nota da modificare
        });

        //Listener per pressione lunga
        noteAdapter.setOnItemLongClickListener((note, anchorView) -> {
            // Chiama l'helper per mostrare il menu
            showContextMenu(note, anchorView);
        });
    }

    // Mostra il menu a tendina
    private void showContextMenu(Note note, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.note_context_menu, popup.getMenu());

        // Logica per cambiare il testo "Pin" / "Unpin"
        MenuItem pinItem = popup.getMenu().findItem(R.id.menu_pin);
        if (note.isPinned) {
            pinItem.setTitle(R.string.menu_unpin);
        } else {
            pinItem.setTitle(R.string.menu_pin);
        }

        // Gestisce i click sul menu
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_pin) {
                togglePinState(note); // Chiama l'helper Pin
                return true;
            } else if (itemId == R.id.menu_delete) {
                confirmDeleteNote(note); // Chiama l'helper Delete
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void togglePinState(Note note) {
        // Crea una nota aggiornata (timestamp invariato) con lo stato 'isPinned' invertito
        Note updatedNote = new Note(
                note.title,
                note.content,
                note.timestamp,
                note.color,
                !note.isPinned
        );
        updatedNote.id = note.id;

        noteViewModel.update(updatedNote);
    }

    private void confirmDeleteNote(Note note) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.menu_delete)
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton(R.string.menu_delete, (dialog, which) -> {
                    noteViewModel.delete(note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Metodo helper per la navigazione
    private void navigateToDetail(@Nullable Note note) {
        NoteDetailFragment detailFragment = new NoteDetailFragment();

        // Prepara i dati da passare al prossimo fragment
        Bundle args = new Bundle();
        if (note == null) {
            args.putInt("NOTE_ID_KEY", -1); // Segnala "nuova nota"
        } else {
            // Passa i dati della nota da modificare
            args.putInt("NOTE_ID_KEY", note.id);
            args.putString("NOTE_TITLE_KEY", note.title);
            args.putString("NOTE_CONTENT_KEY", note.content);
            args.putInt("NOTE_COLOR_KEY", note.color);
            args.putBoolean("NOTE_PINNED_KEY", note.isPinned);
        }
        detailFragment.setArguments(args);

        // Esegue la transazione del fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null) // FONDAMENTALE per il tasto "Indietro"
                .commit();
    }

    // Pulisce il binding per evitare memory leak
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}