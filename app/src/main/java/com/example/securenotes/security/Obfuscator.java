package com.example.securenotes.security;

import android.util.Base64; // Importa Base64

public class Obfuscator {

    private static final String SECRET_KEY = "FIRN32P%&/$FAQ1ds3OF245$";

    public static String xor(String input) {
        // stringa dopo xor ha caratteri strani, per cui meglio passare a byte invece che char.
        byte[] inputBytes;
        try {
            inputBytes = Base64.decode(input, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            // Se non è Base64 valido (es. è la stringa in chiaro originale), usiamo i byte diretti
            inputBytes = input.getBytes();
        }
        char[] key = SECRET_KEY.toCharArray();
        byte[] out = new byte[inputBytes.length];

        for (int i = 0; i < inputBytes.length; i++) {
            out[i] = (byte) (inputBytes[i] ^ key[i % key.length]);
        }

        // Ritorna sempre una stringa Base64 sicura da copiare
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }
}
