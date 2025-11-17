package com.example.securenotes.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentSettingsBinding;
// Importa il nostro Dialog e il Fragment per creare il PIN
import com.example.securenotes.ui.auth.CreatePinFragment;
import com.example.securenotes.ui.auth.EnterPinDialogFragment;

/*
Fragment delle Impostazioni.
Gestisce Timeout e Cambio PIN.
*/
public class SettingsFragment extends Fragment implements
        EnterPinDialogFragment.PinAuthDialogListener { // <-- Implementa l'interfaccia del Dialog

    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;

    public static final String PREFS_NAME = "app_settings";
    public static final String KEY_TIMEOUT = "timeout_ms";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // --- LOGICA TIMEOUT ---
        long currentTimeout = prefs.getLong(KEY_TIMEOUT, 3 * 60 * 1000);

        if (currentTimeout == 1 * 60 * 1000) binding.radio1Min.setChecked(true);
        else if (currentTimeout == 5 * 60 * 1000) binding.radio5Min.setChecked(true);
        else binding.radio3Min.setChecked(true);

        binding.radioGroupTimeout.setOnCheckedChangeListener((group, checkedId) -> {
            long newTimeout = 3 * 60 * 1000;
            if (checkedId == R.id.radio1Min) newTimeout = 1 * 60 * 1000;
            else if (checkedId == R.id.radio5Min) newTimeout = 5 * 60 * 1000;

            prefs.edit().putLong(KEY_TIMEOUT, newTimeout).apply();
            Toast.makeText(getContext(), "Timeout aggiornato!", Toast.LENGTH_SHORT).show();
        });

        // --- LOGICA CAMBIO PIN ---
        binding.btnChangePin.setOnClickListener(v -> {
            // Qui chiamiamo il metodo che mancava!
            showVerifyPinDialog();
        });

        // --- LOGICA BACKUP (Per domani) ---
        binding.btnExportBackup.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Funzionalità Backup in arrivo!", Toast.LENGTH_SHORT).show();
        });
    }

    /*
    METODO AGGIUNTO: Mostra il pop-up con titolo personalizzato
    */
    private void showVerifyPinDialog() {
        EnterPinDialogFragment dialog = new EnterPinDialogFragment();

        // Passiamo il titolo "Insert OLD PIN"
        Bundle args = new Bundle();
        args.putString(EnterPinDialogFragment.ARG_TITLE, "Insert old PIN");
        dialog.setArguments(args);

        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "VerifyPinDialog");
    }

    /*
    CALLBACK: Chiamato quando il vecchio PIN è corretto.
    */
    @Override
    public void onPinAuthDialogSucceeded() {
        // Il vecchio PIN è giusto. Lanciamo la schermata per crearne uno NUOVO.
        CreatePinFragment createFragment = new CreatePinFragment();

        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, createFragment)
                .addToBackStack(null)
                .commit();

        Toast.makeText(getContext(), "Insert new PIN", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}