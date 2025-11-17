package com.example.securenotes;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.annotation.NonNull;
import com.example.securenotes.ui.auth.AuthActivity;
import java.io.File;

public class SecureNotesApplication extends Application implements DefaultLifecycleObserver {

    private static final long LOCK_TIMEOUT_MS = 3 * 60 * 1000; // 3 min per il blocco
    private static final long CACHE_CLEANUP_MS = 10 * 1000;    // 10 sec per pulire file

    private long backgroundTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Task per pulire i file in chiaro rapidamente
    private final Runnable clearCacheTask = this::clearCache;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        clearCache(); // Pulizia all'avvio (crash/reboot recovery)
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        backgroundTime = System.currentTimeMillis();
        // Avvia la pulizia rapida dei file (sicurezza aggressiva)
        handler.postDelayed(clearCacheTask, CACHE_CLEANUP_MS);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // Se l'utente torna subito, annulla la pulizia
        handler.removeCallbacks(clearCacheTask);

        if (backgroundTime > 0) {
            long timePassed = System.currentTimeMillis() - backgroundTime;

            // Se è passato troppo tempo, blocca l'app
            if (timePassed > LOCK_TIMEOUT_MS) {
                clearCache();
                Intent intent = new Intent(this, AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    private void clearCache() {
        try {
            deleteDir(getCacheDir());
        } catch (Exception e) { }
    }

    private void deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) deleteDir(new File(dir, child));
            }
            dir.delete();
        } else if (dir != null && dir.isFile()) {
            dir.delete();
        }
    }
}