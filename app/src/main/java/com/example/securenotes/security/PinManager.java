package com.example.securenotes.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
Gestisce il PIN personalizzato dell'app.
Salva un hash del PIN
in EncryptedSharedPreferences.
*/
public class PinManager {

    private static final String PIN_PREF_FILE_NAME = "secure_notes_pin_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String PIN_MASTER_KEY_ALIAS = "secure_notes_pin_master_key";

    // Helper per ottenere le EncryptedSharedPreferences per il PIN
    private static SharedPreferences getEncryptedPrefs(Context context)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context, PIN_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PIN_PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    // Calcola l'hash SHA-256 del PIN
    private static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Impossibile trovare SHA-256", e);
        }
    }

    // Utility per convertire byte[] in String Esadecimale
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Salva l'hash del PIN (usato in Crea PIN / Cambia PIN)
    public static void savePin(Context context, String pin) {
        try {
            String pinHash = hashPin(pin);
            SharedPreferences prefs = getEncryptedPrefs(context);
            prefs.edit().putString(KEY_PIN_HASH, pinHash).apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    // Controlla se il PIN inserito è corretto (usato in Login)
    public static boolean isPinCorrect(Context context, String pinToVerify) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            String savedHash = prefs.getString(KEY_PIN_HASH, null);
            if (savedHash == null) {
                return false; // Nessun PIN salvato
            }
            String hashToVerify = hashPin(pinToVerify);
            // Confronta gli hash dei PIN
            return savedHash.equals(hashToVerify);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Controlla se l'utente ha mai impostato un PIN (usato all'avvio app)
    public static boolean isPinSet(Context context) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            return prefs.contains(KEY_PIN_HASH);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}