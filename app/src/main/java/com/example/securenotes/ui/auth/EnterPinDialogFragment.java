package com.example.securenotes.ui.auth;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.securenotes.R;
import com.example.securenotes.security.PinManager;

/*
Questo DialogFragment (pop-up) usa il metodo
'setTargetFragment' (deprecato ma semplice)
per restituire il risultato.
*/
public class EnterPinDialogFragment extends DialogFragment {

    // Interfaccia "callback" per notificare il fragment chiamante
    public interface PinAuthDialogListener {
        void onPinAuthDialogSucceeded();
    }

    private PinAuthDialogListener listener;

    // Chiave per passare il titolo personalizzato
    public static final String ARG_TITLE = "dialog_title";

    // Aggancia il listener (che sarà il FileArchiveFragment)
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // CORREZIONE BUG:
        // Usa getTargetFragment() (deprecato) per trovare il listener
        // che è stato impostato da FileArchiveFragment.
        if (getTargetFragment() instanceof PinAuthDialogListener) {
            listener = (PinAuthDialogListener) getTargetFragment();
        } else {
            throw new RuntimeException("Il Fragment genitore (target) deve implementare PinAuthDialogListener");
        }
    }

    // Costruisce il pop-up
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_enter_pin_files, null);
        final EditText pinInput = view.findViewById(R.id.editTextPinDialog);

        String title = "Insert PIN";

        // Controlliamo se chi ci ha chiamato ci ha passato un titolo specifico
        if (getArguments() != null && getArguments().containsKey(ARG_TITLE)) {
            title = getArguments().getString(ARG_TITLE);
        }

        builder.setTitle(title)
                .setView(view)
                .setPositiveButton("Unlock", (dialog, id) -> {
                    String pin = pinInput.getText().toString();

                    // Riutilizza PinManager
                    if (PinManager.isPinCorrect(getContext(), pin)) {
                        listener.onPinAuthDialogSucceeded(); // Successo
                    } else {
                        Toast.makeText(getContext(), "Wrong PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    dialog.cancel();
                });

        return builder.create();
    }
}