package com.example.securenotes.ui.note;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentNoteDetailBinding;
import com.example.securenotes.model.Note;
import com.example.securenotes.viewmodel.NoteViewModel;

/*
Gestisce l'editor per creare o modificare una nota.
Implementa anche il salvataggio automatico e la gestione dei colori.
*/
public class NoteDetailFragment extends Fragment {

    private FragmentNoteDetailBinding binding;
    private NoteViewModel noteViewModel;
    private int currentNoteId = -1;
    // Variabili per memorizzare lo stato iniziale (per evitare salvataggi inutili)
    private String originalTitle = "";
    private String originalContent = "";
    private int originalColor;
    private boolean originalIsPinned = false;
    private int currentColor;

    // FLAG che serve a evitare la creazione di note duplicate
    // quando il ciclo di vita (onStop) scatta più volte (fix bug apertura impostazioni tastiera
    //durante scrittura nota che portava a un doppio salvataggio)
    private boolean isNewNote = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNoteDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.note_white);
        originalColor = defaultColor;
        currentColor = defaultColor;

        Bundle args = getArguments();
        if (args != null) {
            currentNoteId = args.getInt("NOTE_ID_KEY", -1);
        }

        if (currentNoteId == -1) {
            isNewNote = true;
        } else {
            isNewNote = false;

            // Carica i dati dal Bundle
            String title = args.getString("NOTE_TITLE_KEY");
            String content = args.getString("NOTE_CONTENT_KEY");
            originalColor = args.getInt("NOTE_COLOR_KEY", defaultColor);
            originalIsPinned = args.getBoolean("NOTE_PINNED_KEY");

            // Aggiorna le variabili originali
            originalTitle = title;
            originalContent = content;
            currentColor = originalColor;

            // Popola la UI
            binding.editTextTitle.setText(title);
            binding.editTextContent.setText(content);
        }

        updateBackgroundColor(currentColor);
        setupColorPaletteListeners();
    }

    // --- SALVATAGGIO AUTOMATICO ---
    @Override
    public void onStop() {
        super.onStop();
        // Si salva SEMPRE quando l'app va in stop
        // Il metodo saveNote() gestirà i duplicati internamente
        saveNote();
    }

    private void saveNote() {
        String currentTitle = binding.editTextTitle.getText().toString().trim();
        String currentContent = binding.editTextContent.getText().toString().trim();

        // Se nulla è cambiato rispetto all'originale (o all'ultimo salvataggio), esce
        if (currentTitle.equals(originalTitle) &&
                currentContent.equals(originalContent) &&
                currentColor == originalColor) {
            return;
        }

        // cancellazione (Se vuota)
        if (currentTitle.isEmpty() && currentContent.isEmpty()) {
            if (currentNoteId != -1) {
                Note noteToDelete = new Note("", "", 0, 0, false);
                noteToDelete.id = currentNoteId;
                noteViewModel.delete(noteToDelete);
            }
            return;
        }

        // Creazione oggetto
        long timestamp = System.currentTimeMillis();
        // Si usa originalIsPinned per non perdere lo stato del pin
        Note note = new Note(currentTitle, currentContent, timestamp, currentColor, originalIsPinned);

        //Salvataggio (Insert o Update)
        if (isNewNote) {
            noteViewModel.insert(note);
            // Se onStop viene chiamato di nuovo, non la inserirà un'altra volta.
            isNewNote = false;

        } else if (currentNoteId != -1) {
            // È una nota esistente: aggiorna
            note.id = currentNoteId;
            noteViewModel.update(note);
        }
        originalTitle = currentTitle;
        originalContent = currentContent;
        originalColor = currentColor;
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
        // Aggiorna subito la UI
        updateBackgroundColor(currentColor);
    }

    // Gestisce sfondo e colore del testo
    private void updateBackgroundColor(int color) {
        binding.getRoot().setBackgroundColor(color);
        if (isColorDark(color)) {
            // Sfondo scuro -> testo bianco
            binding.editTextTitle.setTextColor(Color.WHITE);
            binding.editTextContent.setTextColor(Color.WHITE);
            binding.editTextTitle.setHintTextColor(Color.LTGRAY);
            binding.editTextContent.setHintTextColor(Color.LTGRAY);
        } else {
            // Sfondo chiaro -> testo nero
            binding.editTextTitle.setTextColor(Color.BLACK);
            binding.editTextContent.setTextColor(Color.BLACK);
            binding.editTextTitle.setHintTextColor(Color.GRAY);
            binding.editTextContent.setHintTextColor(Color.GRAY);
        }
    }

    private boolean isColorDark(int color) {
        return ColorUtils.calculateLuminance(color) < 0.5;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}