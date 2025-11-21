package com.example.securenotes.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import android.view.WindowManager;
import com.example.securenotes.R;
import com.example.securenotes.security.Obfuscator;
import com.example.securenotes.security.PinManager;
import com.example.securenotes.ui.dashboard.MainActivity;
import java.util.concurrent.Executor;
import com.scottyab.rootbeer.RootBeer;
import android.app.AlertDialog;
//import necessari per TamperDetection
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;
import java.security.MessageDigest;
import android.content.pm.PackageManager.NameNotFoundException;

/*
1. Controlla se esiste un PIN.
2. Se NO (primo avvio) -> mostra CreatePinFragment
3. Se SÌ -> mostra BiometricPrompt
4. Se Biometria fallisce/annullata -> mostra EnterPinFragment.
*/

public class AuthActivity extends AppCompatActivity implements
        CreatePinFragment.PinCreationListener,
        EnterPinFragment.PinAuthenticationListener {

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private boolean shouldShowBiometricOnResume = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- PROTEZIONE SCREENSHOT E ANTEPRIMA ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_auth);

        if(isDeviceRooted()){
            showFatalErrorAndExit(R.string.device_not_safe, R.string.rooted_device);
            return;
        }

        if (isAppTampered()) {
            showFatalErrorAndExit(R.string.device_not_safe, R.string.tampered_app);
            return;
        }

        setupBiometricAuth();

        if (savedInstanceState == null) {
            if (PinManager.isPinSet(this)) {
                showBiometricPrompt();
            } else {
                // forza la creazione del PIN
                showCreatePinFragment();
            }
        }
    }

    // App torna dal background: imposta il flag
    @Override
    protected void onRestart() {
        super.onRestart();
        if (PinManager.isPinSet(this)) {
            shouldShowBiometricOnResume = true;
        }
    }

    // UI pronta: controlla il flag e mostra il prompt
    @Override
    protected void onResume() {
        super.onResume();
        if (shouldShowBiometricOnResume) {
            shouldShowBiometricOnResume = false;
            showBiometricPrompt();
        }
    }

    // --- Callback dai Fragment ---

    // Chiamato da CreatePinFragment quando il PIN è stato salvato
    @Override
    public void onPinCreated() {
        navigateToMainApp();
    }

    // Chiamato da EnterPinFragment quando il PIN è corretto
    @Override
    public void onPinAuthenticated() {
        navigateToMainApp();
    }

    // Prepara il pop-up biometrico e i suoi callback
    private void setupBiometricAuth() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        navigateToMainApp(); // Sbloccato
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        // Fallback PIN
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            showEnterPinFragment();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // L'impronta non corrisponde, il prompt resta
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.access_sec_notes))
                .setSubtitle(getString(R.string.auth_required))
                .setNegativeButtonText(getString(R.string.use_pin))
                .build();
    }

    private void showBiometricPrompt() {
        // Chiede al sistema se la biometria è pronta
        //Si permette a dispositivi senza auth biometrica di usare l'app col solo PIN
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            if (!isFinishing() && !isDestroyed()) {
                biometricPrompt.authenticate(promptInfo);
            }
        } else {
            // Se c'è un qualsiasi problema (no HW biometrico) passa al PIN
            showEnterPinFragment();
        }
    }

    private void showCreatePinFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_container, new CreatePinFragment())
                .commit();
    }

    private void showEnterPinFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_container, new EnterPinFragment())
                .commit();
    }

    // Avvia l'app principale e chiude il login
    private void navigateToMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Chiude AuthActivity
    }

    private boolean isDeviceRooted() {
        RootBeer rootBeer = new RootBeer(this);
        return rootBeer.isRooted();
    }

    private boolean isAppTampered() {
        try {
            // Ottiene le info sul pacchetto e le firme
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            // Prende la prima firma (il certificato)
            Signature signature = packageInfo.signatures[0];

            // Calcola l'Hash SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(signature.toByteArray());
            String currentSignatureHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);

            // Logga hash (prima in chiaro) di cui fare XOR il cui risultato è in OBFUSCATED_HASH
            //Log.e("TAMPER_CHECK", "CURRENT HASH: " + currentSignatureHash);

            String OBFUSCATED_HASH = "ff+OJq7efIsITsEC734DEQmqpPE7nplCnEQudlcDVzc=";

            //La stringa vera esiste in RAM solo per questo millisecondo.
            String REAL_SIGNATURE_HASH = Obfuscator.xor(OBFUSCATED_HASH);

            //Log.e("HASHXORED", "HASH XORED: " + REAL_SIGNATURE_HASH);

            return !currentSignatureHash.equals(REAL_SIGNATURE_HASH);

        } catch (NameNotFoundException e) {
            // Se non trova il package, è un errore critico (si assume manomissione)
            return true;
        } catch (Exception e) {
            // Se c'è un errore nella crittografia (MessageDigest), si assume manomissione
            return true;
        }
    }

    private void showFatalErrorAndExit(int titleResId, int messageResId) {
        new AlertDialog.Builder(this)
                .setTitle(getString(titleResId))
                .setMessage(getString(messageResId))
                .setCancelable(false) // L'utente NON può cliccare fuori per chiuderlo
                .setPositiveButton(R.string.close_app, (dialog, which) -> {
                    // Chiude l'app completamente
                    finishAffinity();
                    System.exit(0);
                })
                .show();
    }
}