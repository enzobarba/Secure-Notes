package com.example.securenotes.ui.archive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import android.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.example.securenotes.ui.auth.EnterPinFilesDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentFileArchiveBinding;
import com.example.securenotes.viewmodel.FileViewModel;
import java.io.File;
import java.util.concurrent.Executor;

/*
Questo Fragment (Refactoring Fatto).
Delega tutto il lavoro pesante (I/O, Cripto)
al FileViewModel.
*/
public class FileArchiveFragment extends Fragment implements EnterPinFilesDialogFragment.PinAuthDialogListener{

    private FragmentFileArchiveBinding binding;
    private FileAdapter fileAdapter;
    private FileViewModel fileViewModel; // Il nostro "ponte"

    // Oggetti per l'autenticazione secondaria (UI 4)
    private Executor biometricExecutor;
    private BiometricPrompt biometricPrompt;

    private File fileToOpen;

    // Launcher per il selettore di file
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Registra il "callback" per il selettore di file
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            // Chiedi al ViewModel di importare
                            fileViewModel.importFile(fileUri);
                        }
                    }
                }
        );

        // Prepara il gestore biometrico
        setupBiometricPrompt();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileArchiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Imposta l'Adapter
        fileAdapter = new FileAdapter();
        binding.recyclerViewFiles.setAdapter(fileAdapter);

        // 2. Collega il FileViewModel
        fileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        // 3. Imposta il click sul FAB
        binding.fabAddFile.setOnClickListener(v -> launchFilePicker());

        // 4. Click sul file -> lancia l'autenticazione
        fileAdapter.setOnItemClickListener(file -> {
            // Requisito UI 4: Chiedi l'auth *prima* di aprire
            authenticateAndOpenFile(file);
        });

        fileAdapter.setOnItemLongClickListener((file, anchorView) -> {
            showFileContextMenu(file, anchorView);
        });

        // 5. Osserva i LiveData del ViewModel
        observeViewModel();

        // 6. Carica la lista iniziale
        fileViewModel.refreshFileList();
    }

    // Raggruppa gli osservatori LiveData
    private void observeViewModel() {

        // Osserva la lista dei file
        fileViewModel.fileList.observe(getViewLifecycleOwner(), files -> {
            if (files != null) {
                fileAdapter.submitList(files);
            }
        });

        // Osserva i messaggi (es. "File importato!")
        fileViewModel.toastMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                fileViewModel.onToastMessageShown();
            }
        });

        // Osserva l'URI del file decriptato (pronto per essere aperto)
        fileViewModel.decryptedFileUri.observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                openFileWithIntent(uri);
                fileViewModel.onFileOpened();
            }
        });
    }

    // Lancia l'Intent per scegliere un file
    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    // Prepara il BiometricPrompt (solo l'executor)
    private void setupBiometricPrompt() {
        biometricExecutor = ContextCompat.getMainExecutor(requireContext());
    }

    private void authenticateAndOpenFile(File encryptedFile) {

        // 1. Salva il file che l'utente vuole aprire
        //    nella nostra variabile di classe.
        this.fileToOpen = encryptedFile;

        // 2. Prepara il "callback" (cosa fare DOPO l'autenticazione)
        BiometricPrompt.AuthenticationCallback callback =
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        // SUCCESSO!
                        // Chiamiamo il ViewModel (passando il file
                        // che abbiamo salvato).
                        fileViewModel.decryptAndPrepareFile(fileToOpen);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        // Se l'utente preme "Annulla" o "Usa PIN"...
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {

                            // ...mostra il NOSTRO pop-up del PIN.
                            showPinDialog();
                        }
                    }
                };

        // 3. Crea e mostra il prompt
        biometricPrompt = new BiometricPrompt(this, biometricExecutor, callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verifica Identità")
                .setSubtitle("Autenticazione richiesta per aprire il file")
                .setNegativeButtonText("Usa PIN") // Testo aggiornato
                .build();

        if (canUseBiometrics()) {
            // Se sì, mostra il prompt
            biometricPrompt.authenticate(promptInfo);
        } else {
            // Se no (es. emulatore senza impronte),
            // salta direttamente al PIN Dialog.
            showPinDialog();
        }
    }


    //Questo metodo helper crea e mostra il nostro nuovo DialogFragment.

    private void showPinDialog() {
        EnterPinFilesDialogFragment dialog = new EnterPinFilesDialogFragment();

        // 'this' (FileArchiveFragment) è il "genitore"
        // che riceverà il callback.
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "EnterPinDialog");
    }

    /*Viene chiamato dal 'EnterPinDialogFragment'
    quando l'utente inserisce il PIN corretto.
    */
    @Override
    public void onPinAuthDialogSucceeded() {
        // SUCCESSO! L'utente ha inserito il PIN.

        // Controlliamo se abbiamo un file "in attesa"
        if (fileToOpen != null) {
            // Diciamo al ViewModel di decriptare
            fileViewModel.decryptAndPrepareFile(fileToOpen);

            // Resettiamo il file in attesa per sicurezza
            fileToOpen = null;
        }
    }

    // 2. Lancia l'Intent per aprire il file (chiamato dal LiveData)
    private void openFileWithIntent(Uri fileUri) {
        try {
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setData(fileUri);
            // DA' il permesso temporaneo all'altra app di leggere
            openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(openIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Nessuna app trovata per aprire questo file", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    Questo metodo crea e mostra il nostro menu
    per i file.
    */
    private void showFileContextMenu(File file, View anchorView) {
        // 1. Crea un PopupMenu
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);

        // 2. "Gonfia" (crea) il menu usando il nostro NUOVO
        //    file 'file_context_menu.xml' (che ha solo "Delete").
        popup.getMenuInflater().inflate(R.menu.file_context_menu, popup.getMenu());

        // 3. Imposta il listener per i click
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_delete) {
                // Utente ha cliccato "Elimina"
                confirmDeleteFile(file); // Chiedi conferma
                return true;
            }
            return false;
        });

        // 4. Mostra il menu
        popup.show();
    }


    //Mostra un pop-up di conferma "Sei sicuro?"
    private void confirmDeleteFile(File file) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.menu_delete) // "Delete"
                .setMessage("Are you sure you want to delete this file?\n\n" + file.getName())
                .setPositiveButton(R.string.menu_delete, (dialog, which) -> {
                    // Se l'utente clicca "Elimina",
                    // chiama il nostro nuovo metodo del ViewModel.
                    fileViewModel.deleteFile(file);
                })
                .setNegativeButton("Cancel", null) // "Annulla"
                .show(); // Mostra il pop-up
    }

    private boolean canUseBiometrics() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // Pulisci il binding
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}