package com.example.securenotes.ui.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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