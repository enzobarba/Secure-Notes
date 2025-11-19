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
import com.example.securenotes.security.PinManager;
import com.example.securenotes.ui.dashboard.MainActivity;
import java.util.concurrent.Executor;

/*

1. Controlla se esiste un PIN.
2. Se NO (primo avvio) -> mostra CreatePinFragment.
3. Se SÌ -> mostra BiometricPrompt.
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

        setupBiometricAuth();

        if (savedInstanceState == null) {
            // Controlla se l'utente ha un PIN
            if (PinManager.isPinSet(this)) {
                // mostra il pop-up di impronta/volto
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

        // Testo del pop-up
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.access_sec_notes))
                .setSubtitle(getString(R.string.auth_required))
                .setNegativeButtonText(getString(R.string.use_pin))
                .build();
    }

    // Mostra il pop-up
    private void showBiometricPrompt() {
        // 1. Chiedi al sistema se la biometria è pronta
        //Si permette a dispositivi senza auth biometrica di usare l'app col pin
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            // 2a. Se è tutto ok, mostra il prompt
            if (!isFinishing() && !isDestroyed()) {
                biometricPrompt.authenticate(promptInfo);
            }
        } else {
            // 2b. Se c'è un QUALSIASI problema (niente hardware, niente impronte, ecc.)
            // vai SUBITO al PIN. Non lasciare lo schermo bianco.
            showEnterPinFragment();
        }
    }

    // Carica il fragment per creare PIN
    private void showCreatePinFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_container, new CreatePinFragment())
                .commit();
    }

    // Carica il fragment per inserire PIN
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
}