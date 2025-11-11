package com.example.securenotes.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;
import androidx.core.content.ContextCompat;
import com.example.securenotes.R;

import com.example.securenotes.databinding.FragmentNoteDetailBinding;
import com.example.securenotes.model.Note;
import com.example.securenotes.viewmodel.NoteViewModel;

import java.util.Objects;

/*
Questo Fragment gestisce l'editor per creare o modificare una nota.
Implementa il salvataggio automatico.
*/
public class NoteDetailFragment extends Fragment {

    private FragmentNoteDetailBinding binding;
    private NoteViewModel noteViewModel;

    // 'currentNoteId' tiene traccia della nota che stiamo modificando.
    // -1 significa "nota nuova".
    private int currentNoteId = -1;
    private String originalTitle = "";
    private String originalContent = "";
    private int currentColor;
    private int originalColor;

    // "Gonfia" il layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNoteDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Collega la logica e carica i dati
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        int defaultColor = ContextCompat.getColor(requireContext(), R.color.note_white);
        originalColor = defaultColor;
        currentColor = defaultColor;
        // Recupera i dati passati dal NoteListFragment
        Bundle args = getArguments();

        if (args != null) {
            currentNoteId = args.getInt("NOTE_ID_KEY", -1);
        }

        if (currentNoteId != -1) {
            // È una nota esistente: riempie i campi
            assert args != null;
            String title = args.getString("NOTE_TITLE_KEY");
            String content = args.getString("NOTE_CONTENT_KEY");
            originalColor = args.getInt("NOTE_COLOR_KEY");
            originalTitle = title;
            originalContent = content;
            binding.editTextTitle.setText(title);
            binding.editTextContent.setText(content);
            currentColor = originalColor;
        }
        // else: È una nota nuova, lascia i campi vuoti.
        binding.getRoot().setBackgroundColor(currentColor);
        setupColorPaletteListeners();
    }

    //SALVATAGGIO AUTOMATICO
    @Override
    public void onStop() {
        super.onStop();
        saveNote();
    }

    // Pulisci il binding
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void saveNote() {
        String title = binding.editTextTitle.getText().toString().trim();
        String content = binding.editTextContent.getText().toString().trim();

        if (title.equals(originalTitle) && content.equals(originalContent) && currentColor == originalColor) {
            // L'utente non ha cambiato nulla.
            // NON verrà generato un nuovo timestamp.
            // NON verrà fatta nessuna operazione sul database.
            return;
        }
        // Se la nota è vuota, viene cancellata.
        if (title.isEmpty() && content.isEmpty()) {
            if (currentNoteId != -1) {
                // Per cancellare, passiamo una nota fittizia solo con l'ID
                Note noteToDelete = new Note("", "", 0, 0);
                noteToDelete.id = currentNoteId;
                noteViewModel.delete(noteToDelete);
            }
            return; // Non salvare note vuote
        }

        // Crea la nota da salvare
        Note note = new Note(title, content, System.currentTimeMillis(), currentColor);

        if (currentNoteId == -1) {
            // È una nota nuova: inserisci
            noteViewModel.insert(note);
        } else {
            // È una nota esistente: aggiorna
            note.id = currentNoteId; // Imposta l'ID per l'aggiornamento
            noteViewModel.update(note);
        }
    }

    private void setupColorPaletteListeners() {
        binding.colorWhite.setOnClickListener(v -> setCurrentColor(R.color.note_white));
        binding.colorYellow.setOnClickListener(v -> setCurrentColor(R.color.note_yellow));
        binding.colorGreen.setOnClickListener(v -> setCurrentColor(R.color.note_green));
        binding.colorBlue.setOnClickListener(v -> setCurrentColor(R.color.note_blue));
        binding.colorRed.setOnClickListener(v -> setCurrentColor(R.color.note_red));
    }

    private void setCurrentColor(int colorResId) {
        currentColor = ContextCompat.getColor(requireContext(), colorResId);
        binding.getRoot().setBackgroundColor(currentColor);
    }
}