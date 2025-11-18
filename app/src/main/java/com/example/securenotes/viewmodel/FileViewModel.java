package com.example.securenotes.viewmodel;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.securenotes.repository.FileRepository;
import java.io.File;
import java.util.List;

public class FileViewModel extends AndroidViewModel {

    private final FileRepository fileRepository;
    private final MutableLiveData<List<File>> _fileList = new MutableLiveData<>();
    public final LiveData<List<File>> fileList = _fileList;
    private final MutableLiveData<Uri> _decryptedFileUri = new MutableLiveData<>();
    public final LiveData<Uri> decryptedFileUri = _decryptedFileUri;
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public final LiveData<String> toastMessage = _toastMessage;

    public FileViewModel(@NonNull Application application) {
        super(application);
        this.fileRepository = new FileRepository();
    }

    public void refreshFileList() {
        fileRepository.getExecutor().execute(() -> {
            List<File> files = fileRepository.loadFiles(getApplication());
            _fileList.postValue(files);
        });
    }

    public void importFile(Uri fileUri) {
        fileRepository.getExecutor().execute(() -> {
            try {
                fileRepository.encryptFile(getApplication(), fileUri);
                refreshFileList();
                _toastMessage.postValue("File successfully imported!");
            } catch (Exception e) {
                // Logga anche gli errori di importazione
                _toastMessage.postValue("Importation failed: " + e.getMessage());
            }
        });
    }

    // Metodo di decrittografia con logging degli errori
    public void decryptAndPrepareFile(File encryptedFile) {
        fileRepository.getExecutor().execute(() -> {
            try {
                File tempFile = fileRepository.decryptFile(getApplication(), encryptedFile);
                Uri fileUri = FileProvider.getUriForFile(
                        getApplication(),
                        "com.example.securenotes.provider",
                        tempFile
                );
                _decryptedFileUri.postValue(fileUri);
            } catch (Exception e) {
                // CORREZIONE DI DEBUG:
                // 1. Stampa l'errore completo nel Logcat
                // 2. Mostra il vero messaggio di errore all'utente
                _toastMessage.postValue("Error: " + e.getMessage());
            }
        });
    }

    // Elimina un file (in background)
    public void deleteFile(File fileToDelete) {
        fileRepository.getExecutor().execute(() -> {
            try {
                if (fileRepository.deleteFile(fileToDelete)) {
                    // Successo: ricarica la lista e invia un Toast
                    refreshFileList();
                    _toastMessage.postValue("File deleted");
                } else {
                    _toastMessage.postValue("Eliminazione fallita.");
                }
            } catch (Exception e) {
                _toastMessage.postValue("Error during deletation.");
            }
        });
    }

    public void onFileOpened(){
        _decryptedFileUri.postValue(null);
    }

    public void onToastMessageShown() {
        _toastMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        fileRepository.shutdown();
    }
}