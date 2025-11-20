package com.example.securenotes.repository;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
gestisce l'I/O e la cifratura dei File.
*/
public class FileRepository {

    private final ExecutorService executor;
    private static final String SECURE_DIR_NAME = "secure_files";
    private static final String FILE_MASTER_KEY_ALIAS = "secure_notes_file_master_key";

    public FileRepository() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    private File getSecureDir(Context context) {
        File dir = new File(context.getFilesDir(), SECURE_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public List<File> loadFiles(Context context) {
        File[] files = getSecureDir(context).listFiles();
        if (files != null) {
            return Arrays.asList(files);
        }
        return null;
    }

    // Cripta e salva un nuovo file
    public File encryptFile(Context context, Uri fileUri)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context, FILE_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        String fileName = getFileNameFromUri(context, fileUri);
        File outputFile = new File(getSecureDir(context), fileName);

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                outputFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
        OutputStream outputStream = encryptedFile.openFileOutput();

        // Loop di copia standard e sicuro
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();

        return outputFile;
    }

    // Decripta un file in una copia temporanea
    public File decryptFile(Context context, File encryptedFile)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context, FILE_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        EncryptedFile fileToRead = new EncryptedFile.Builder(
                context,
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        File tempFile = new File(context.getCacheDir(), encryptedFile.getName());

        InputStream inputStream = fileToRead.openFileInput();
        OutputStream outputStream = new FileOutputStream(tempFile);

        // Loop di copia standard e sicuro
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }

        //Svuota il buffer prima di chiudere
        outputStream.flush();
        outputStream.close();
        inputStream.close();

        return tempFile;
    }

    public boolean deleteFile(File fileToDelete) {
        if (!fileToDelete.exists() || !fileToDelete.isFile()) {
            return false;
        }
        return fileToDelete.delete();
    }

    // Helper per ottenere il nome del file dall'Uri
    private String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        if (fileName == null) {
            fileName = "imported_file_" + System.currentTimeMillis();
        }
        return fileName;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}