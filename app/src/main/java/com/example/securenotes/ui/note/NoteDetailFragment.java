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
Questo Fragment gestisce l'editor per creare o modificare una nota.
Implementa il salvataggio automatico e la gestione dei colori.
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

    // Variabile per il colore attuale
    private int currentColor;

    // FLAG FONDAMENTALE: Serve a evitare la creazione di note duplicate
    // quando il ciclo di vita (onStop) scatta più volte.
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

        // Imposta il colore di default
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.note_white);
        originalColor = defaultColor;
        currentColor = defaultColor;

        Bundle args = getArguments();
        if (args != null) {
            currentNoteId = args.getInt("NOTE_ID_KEY", -1);
        }

        if (currentNoteId == -1) {
            // CASO 1: NOTA NUOVA
            // Impostiamo il flag a true.
            isNewNote = true;
        } else {
            // CASO 2: NOTA ESISTENTE
            isNewNote = false;

            // Carichiamo i dati dal Bundle
            // (Nota: assert args != null non serve se abbiamo controllato if (currentNoteId != -1))
            String title = args.getString("NOTE_TITLE_KEY");
            String content = args.getString("NOTE_CONTENT_KEY");
            originalColor = args.getInt("NOTE_COLOR_KEY", defaultColor);
            originalIsPinned = args.getBoolean("NOTE_PINNED_KEY");

            // Aggiorniamo le variabili "originali"
            originalTitle = title;
            originalContent = content;
            currentColor = originalColor;

            // Popoliamo la UI
            binding.editTextTitle.setText(title);
            binding.editTextContent.setText(content);
        }

        // Applichiamo il colore (sfondo e testo)
        updateBackgroundColor(currentColor);

        setupColorPaletteListeners();
    }

    // --- SALVATAGGIO AUTOMATICO ---
    @Override
    public void onStop() {
        super.onStop();
        // Rimosso il controllo 'if (isNewNote && ...)' che avevi messo.
        // Vogliamo salvare SEMPRE quando l'app va in stop (anche tasto Home).
        // Il metodo saveNote() gestirà i duplicati internamente.
        saveNote();
    }

    private void saveNote() {
        String currentTitle = binding.editTextTitle.getText().toString().trim();
        String currentContent = binding.editTextContent.getText().toString().trim();

        // 1. CONTROLLO DI GUARDIA
        // Se nulla è cambiato rispetto all'originale (o all'ultimo salvataggio), esci.
        if (currentTitle.equals(originalTitle) &&
                currentContent.equals(originalContent) &&
                currentColor == originalColor) {
            return;
        }

        // 2. CANCELLAZIONE (Se vuota)
        if (currentTitle.isEmpty() && currentContent.isEmpty()) {
            if (currentNoteId != -1) {
                Note noteToDelete = new Note("", "", 0, 0, false);
                noteToDelete.id = currentNoteId;
                noteViewModel.delete(noteToDelete);
            }
            return;
        }

        // 3. CREAZIONE OGGETTO
        long timestamp = System.currentTimeMillis();
        // Usiamo originalIsPinned per non perdere lo stato del pin
        Note note = new Note(currentTitle, currentContent, timestamp, currentColor, originalIsPinned);

        // 4. SALVATAGGIO (Insert o Update)
        if (isNewNote) {
            // È una nota nuova: INSERISCI
            noteViewModel.insert(note);

            // IMPORTANTE: Ora la nota esiste. Impostiamo il flag a false.
            // Se onStop viene chiamato di nuovo, non la inserirà un'altra volta.
            isNewNote = false;

        } else if (currentNoteId != -1) {
            // È una nota esistente: AGGIORNA
            note.id = currentNoteId;
            noteViewModel.update(note);
        }

        // 5. AGGIORNAMENTO STATO
        // Aggiorniamo le variabili "original" al valore attuale.
        // Così se saveNote() viene richiamato subito dopo, il controllo n.1 ci bloccherà.
        originalTitle = currentTitle;
        originalContent = currentContent;
        originalColor = currentColor;
    }

    // --- GESTIONE COLORI ---

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

    // Metodo avanzato per gestire sfondo E colore del testo
    private void updateBackgroundColor(int color) {
        binding.getRoot().setBackgroundColor(color);

        // Controlla se il colore è scuro
        if (isColorDark(color)) {
            // Sfondo scuro -> Testo Bianco
            binding.editTextTitle.setTextColor(Color.WHITE);
            binding.editTextContent.setTextColor(Color.WHITE);
            binding.editTextTitle.setHintTextColor(Color.LTGRAY);
            binding.editTextContent.setHintTextColor(Color.LTGRAY);
        } else {
            // Sfondo chiaro -> Testo Nero
            binding.editTextTitle.setTextColor(Color.BLACK);
            binding.editTextContent.setTextColor(Color.BLACK); // o DKGRAY
            binding.editTextTitle.setHintTextColor(Color.GRAY);
            binding.editTextContent.setHintTextColor(Color.GRAY);
        }
    }

    // Helper per la luminosità
    private boolean isColorDark(int color) {
        return ColorUtils.calculateLuminance(color) < 0.5;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}