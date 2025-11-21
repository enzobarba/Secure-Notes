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
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.securenotes.viewmodel.FileViewModel;
import com.example.securenotes.viewmodel.NoteViewModel;
import com.example.securenotes.security.BackupWorker;

import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentSettingsBinding;
// Importa il nostro Dialog e il Fragment per creare il PIN
import com.example.securenotes.ui.auth.CreatePinFragment;
import com.example.securenotes.ui.auth.EnterPinDialogFragment;


public class SettingsFragment extends Fragment implements
        EnterPinDialogFragment.PinAuthDialogListener,
        BackupPasswordDialogFragment.BackupPasswordListener{ // <-- Implementa l'interfaccia del Dialog

    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;

    private NoteViewModel noteViewModel;
    private FileViewModel fileViewModel;

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

        // Per controllare i dati
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        fileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        // Sveglia i LiveData (fix bug di solo note non backupate -> dava errore 'no data to backup')
        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {});
        fileViewModel.fileList.observe(getViewLifecycleOwner(), files -> {});
        // Forza il caricamento dei file per essere sicuri di avere il numero aggiornato
        fileViewModel.refreshFileList();

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
            Toast.makeText(getContext(), R.string.timeout_updated, Toast.LENGTH_SHORT).show();

        });

        binding.btnChangePin.setOnClickListener(v -> {
            showVerifyPinDialog();
        });

        binding.btnExportBackup.setOnClickListener(v -> {
            boolean hasNotes = false;
            boolean hasFiles = false;
            if (noteViewModel.getAllNotes().getValue() != null &&
                    !noteViewModel.getAllNotes().getValue().isEmpty()) {
                hasNotes = true;
            }
            if (fileViewModel.fileList.getValue() != null &&
                    !fileViewModel.fileList.getValue().isEmpty()) {
                hasFiles = true;
            }
            if (!hasNotes && !hasFiles) {
               Toast.makeText(getContext(), R.string.no_data_to_backup, Toast.LENGTH_SHORT).show();
            } else {
                showBackupPasswordDialog();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //Callback chiamata quando il vecchio PIN è corretto.
    @Override
    public void onPinAuthDialogSucceeded() {
        CreatePinFragment createFragment = new CreatePinFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, createFragment)
                .addToBackStack(null)
                .commit();
        Toast.makeText(getContext(), R.string.create_pin, Toast.LENGTH_SHORT).show();
    }

    //lancia il workManager
    @Override
    public void onBackupPasswordSet(String password) {
        Toast.makeText(getContext(), R.string.backup_running, Toast.LENGTH_SHORT).show();

        // prepara i dati da inviare al Worker (la password)
        Data inputData = new Data.Builder()
                .putString(BackupWorker.KEY_PASSWORD, password)
                .build();

        // crea la richiesta
        OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setInputData(inputData) // password
                .build();

        // mette in coda
        WorkManager.getInstance(requireContext()).enqueue(backupRequest);

        // osserva lo stato per sapere quando finisce
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(backupRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(getContext(), R.string.backup_saved, Toast.LENGTH_SHORT).show();
                    } else if (workInfo != null && workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(getContext(), R.string.backup_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Mostra pop-up con titolo personalizzato
    private void showVerifyPinDialog() {
        EnterPinDialogFragment dialog = new EnterPinDialogFragment();
        // Passa il titolo "Insert old PIN"
        Bundle args = new Bundle();
        args.putString(EnterPinDialogFragment.ARG_TITLE, getString(R.string.insert_old_pin));
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "VerifyPinDialog");
    }

    private void showBackupPasswordDialog() {
        BackupPasswordDialogFragment dialog = new BackupPasswordDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "BackupDialog");
    }

}