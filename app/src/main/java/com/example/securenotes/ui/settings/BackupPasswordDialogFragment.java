package com.example.securenotes.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.securenotes.R;


public class BackupPasswordDialogFragment extends DialogFragment {

    public interface BackupPasswordListener {
        void onBackupPasswordSet(String password);
    }

    private BackupPasswordListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof BackupPasswordListener) {
            listener = (BackupPasswordListener) getTargetFragment();
        } else {
            throw new RuntimeException("Parent must implement BackupPasswordListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_backup_password, null);

        final EditText input = view.findViewById(R.id.editTextBackupPassword);

        builder.setView(view)
                .setTitle("Create Backup")
                .setPositiveButton("Start", null)
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        //  Creiamo il Dialog
        AlertDialog dialog = builder.create();


        // eseguito quando il dialog appare a schermo.
        dialog.setOnShowListener(dialogInterface -> {

            // 3. Recupera il pulsante "Avvia"
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // 4. Imposta il click listener personalizzato.
            //    Questo sovrascrive il comportamento di chiusura automatica.
            button.setOnClickListener(view1 -> {
                String password = input.getText().toString();

                // --- VALIDAZIONE SICUREZZA ---
                if (!password.isEmpty() && isValidPassword(password)) {
                    // Invia la password e chiudi manualmente il dialog.
                    listener.onBackupPasswordSet(password);
                    dialog.dismiss();
                } else {
                    // Mostra errore e NON chiudere il dialog.
                    input.setError("Weak password! Requested: \n- 12 character\n- 1 Upper\n- 1 Number\n- 1 Special Character (@#$%^&+=!)");
                }
            });
        });
        return dialog;
    }

    private boolean isValidPassword(String password) {
        // 1. Lunghezza minima 12
        if (password.length() < 12) return false;

        // 2. Almeno una Maiuscola
        if (!password.matches(".*[A-Z].*")) return false;

        // 3. Almeno un Numero
        if (!password.matches(".*[0-9].*")) return false;

        // 4. Almeno un Carattere Speciale
        if (!password.matches(".*[@#$%^&+=!.].*")) return false;

        return true;
    }
}