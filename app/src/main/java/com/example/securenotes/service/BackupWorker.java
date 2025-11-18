package com.example.securenotes.service;

import android.content.ContentValues; // Per MediaStore
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.securenotes.db.AppDatabase;
import com.example.securenotes.model.Note;
import com.example.securenotes.repository.FileRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class BackupWorker extends Worker {

    public static final String KEY_PASSWORD = "backup_password";
    private static final String TAG = "BackupWorker"; // Tag per il Logcat

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 10000;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String password = getInputData().getString(KEY_PASSWORD);
        if (password == null) {
            return Result.failure();
        }

        // Variabili per pulire in caso di errore
        Uri backupUri = null;

        try {
            // 1. Prepara il nome del file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "SecureNotes_Backup_" + timeStamp + ".enc";

            // 2. Crea l'OutputStream usando MediaStore (Funziona su Android 10+)
            //    Questo metodo non richiede permessi di scrittura esterni.
            OutputStream fos;

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream"); // Tipo binario
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/SecureNotes"); // Crea una sottocartella

            // Inserisci il record nel sistema e ottieni un URI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backupUri = getApplicationContext().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (backupUri == null) {
                throw new Exception("Impossibile creare file in MediaStore");
            }

            // Apri il flusso di scrittura verso quell'URI
            fos = getApplicationContext().getContentResolver().openOutputStream(backupUri);

            if (fos == null) {
                throw new Exception("Impossibile aprire OutputStream");
            }

            // 3. Genera Salt e IV
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);

            // 4. Cripta
            SecretKey secretKey = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            // 5. Scrivi Salt e IV all'inizio
            fos.write(salt);
            fos.write(iv);

            // 6. Zip e Scrivi
            ZipOutputStream zos = new ZipOutputStream(new CipherOutputStream(fos, cipher));
            backupNotes(zos);
            backupFiles(zos);
            zos.close(); // Chiude tutto a cascata
            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        byte[] tmp = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(tmp, "AES");
    }

    private void backupNotes(ZipOutputStream zos) throws Exception {
        List<Note> notes = AppDatabase.getInstance(getApplicationContext()).noteDao().getAllNotesDirect();

        StringBuilder sb = new StringBuilder();
        if (notes != null) {
            for (Note note : notes) {
                sb.append("Title: ").append(note.title).append("\n");
                sb.append("Content: ").append(note.content).append("\n\n");
            }
        }
        ZipEntry entry = new ZipEntry("notes.txt");
        zos.putNextEntry(entry);
        zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void backupFiles(ZipOutputStream zos) throws Exception {
        FileRepository repo = new FileRepository();
        List<File> files = repo.loadFiles(getApplicationContext());

        if (files != null) {
            for (File encryptedFile : files) {
                try {
                    File tempDecrypted = repo.decryptFile(getApplicationContext(), encryptedFile);

                    ZipEntry entry = new ZipEntry("files/" + encryptedFile.getName());
                    zos.putNextEntry(entry);

                    FileInputStream fis = new FileInputStream(tempDecrypted);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);

                    fis.close();
                    zos.closeEntry();
                    tempDecrypted.delete();

                } catch (Exception e) { e.printStackTrace(); }
            }
        }

    }

}