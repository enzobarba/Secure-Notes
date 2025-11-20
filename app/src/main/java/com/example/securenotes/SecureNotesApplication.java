package com.example.securenotes;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.annotation.NonNull;
import com.example.securenotes.ui.auth.AuthActivity;
import java.io.File;

public class SecureNotesApplication extends Application implements DefaultLifecycleObserver {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_TIMEOUT = "timeout_ms";

    private static final long DEFAULT_TIMEOUT = 3 * 60 * 1000;
    private static final long CACHE_CLEANUP_MS = 10 * 1000;

    private long backgroundTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clearCacheTask = this::clearCache;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        clearCache();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        backgroundTime = System.currentTimeMillis();
        handler.postDelayed(clearCacheTask, CACHE_CLEANUP_MS);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        handler.removeCallbacks(clearCacheTask);

        if (backgroundTime > 0) {
            long timePassed = System.currentTimeMillis() - backgroundTime;

            // Legge il timeout scelto dall'utente (o usa il default)
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long userTimeout = prefs.getLong(KEY_TIMEOUT, DEFAULT_TIMEOUT);

            if (timePassed > userTimeout) {
                clearCache();
                Intent intent = new Intent(this, AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    private void clearCache() {
        try { deleteDir(getCacheDir()); } catch (Exception e) { }
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