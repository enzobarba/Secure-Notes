package com.example.securenotes.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import android.util.Base64;

/*
Gestisce la chiave di crittografia del database.
Usa EncryptedSharedPreferences (Jetpack Security) per
salvare una passphrase generata casualmente,
protetta dall'Android Keystore.
*/
public class SecurityDbManager {

    private static final String PREF_FILE_NAME = "secure_notes_prefs";
    private static final String KEY_DB_PASSPHRASE = "db_passphrase";
    //Alias univoco per MasterKey DB, senza esso si può usare MasterKey.Builder(context)
    //ma userebbe alias predefinito _androidx_security_master_key_
    private static final String DB_MASTER_KEY_ALIAS = "secure_notes_db_master_key";

    // Ottiene o crea la passphrase per SQLCipher
    public static synchronized byte[] getDatabasePassphrase(Context context)
            throws GeneralSecurityException, IOException {

        // Crea o ottiene la MasterKey dall'Android Keystore
        MasterKey masterKey = new MasterKey.Builder(context, DB_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        // Apre le SharedPreferences criptate
        SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        // Cerca la passphrase salvata
        String base64Passphrase = sharedPreferences.getString(KEY_DB_PASSPHRASE, null);

        if (base64Passphrase == null) {
            // Non trovata (al primo avvio): Genera una nuova passphrase
            byte[] newPassphrase = new byte[32]; // 256 bit
            new SecureRandom().nextBytes(newPassphrase);

            base64Passphrase = Base64.encodeToString(newPassphrase, Base64.NO_WRAP);
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, base64Passphrase).apply();

            return newPassphrase;
        } else {
            // Trovata: Decodifica e restituisci la passphrase esistente
            return Base64.decode(base64Passphrase, Base64.NO_WRAP);
        }
    }
}