package com.example.securenotes.ui.note;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentNoteListBinding;
import com.example.securenotes.model.Note;
import com.example.securenotes.viewmodel.NoteViewModel;


public class NoteListFragment extends Fragment {

    private FragmentNoteListBinding binding;
    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;

    // Interfaccia callback per dire a MainActivity di navigare
    public interface NoteNavigationListener {
        void navigateToDetail(int noteId, String title, String content, int color, boolean isPinned);
    }
    private NoteNavigationListener navigationListener;

    // Aggancia il listener all'Activity
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Controlla che MainActivity implementi l'interfaccia
        if (context instanceof NoteNavigationListener) {
            navigationListener = (NoteNavigationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NoteNavigationListener");
        }
    }

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

        // imposta Adapter e RecyclerView
        noteAdapter = new NoteAdapter();
        binding.recyclerViewNotes.setAdapter(noteAdapter);

        // collega il ViewModel
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // osserva i dati LiveData dal ViewModel
        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            noteAdapter.submitList(notes);
        });

        // Pulsante "+" (chiama MainActivity)
        binding.fabAddNote.setOnClickListener(v -> {
            // 'navigationListener' è l'Activity
            navigationListener.navigateToDetail(-1, "", "", 0, false);
        });

        // Click corto su una nota (chiama MainActivity)
        noteAdapter.setOnItemClickListener(note -> {
            navigationListener.navigateToDetail(
                    note.id, note.title, note.content, note.color, note.isPinned
            );
        });

        noteAdapter.setOnItemLongClickListener((note, anchorView) -> {
            showContextMenu(note, anchorView);
        });
    }

    // Pulisce il binding per evitare memory leak
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showContextMenu(Note note, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.note_context_menu, popup.getMenu());
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
                togglePinState(note);
                return true;
            } else if (itemId == R.id.menu_delete) {
                confirmDeleteNote(note);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // Logica per pinnare che non aggiorna il timestamp
    private void togglePinState(Note note) {
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
                .setMessage(R.string.delete_note)
                .setPositiveButton(R.string.menu_delete, (dialog, which) -> {
                    noteViewModel.delete(note);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

}