# Ignora i warning generici
-ignorewarnings

# Mantieni classi di AndroidX
-keep class androidx.** { *; }

# Mantieni classi usate dalla Biometric API
-keep class androidx.biometric.** { *; }

# Mantieni i tuoi Modelli (per Room)
-keep class com.example.securenotes.model.** { *; }

# Mantieni la tua Application (Entry point)
-keep class com.example.securenotes.SecureNotesApplication { *; }

# Mantieni i Worker (per il Backup)
-keep class com.example.securenotes.worker.** { *; }

# Mantieni SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Mantieni Jetpack Security
-keep class androidx.security.crypto.** { *; }

# Mantieni Room
-keepclassmembers class * {
    @androidx.room.* <methods>;
}