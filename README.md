# 🔒 SecureNotes - Encrypted Data Vault (University Project)

This Android application, developed in Java, manages personal notes and stores sensitive files (e.g., PDFs, images). Its primary focus is to deliver maximum data security through rigorous persistence and runtime standards, adhering to modern Android development best practices.

## 🚀 Project Overview and Goals

This project successfully implements all core requirements (MVVM, Jetpack Security, SQLCipher, WorkManager) defined in the assignment brief.

### Core Features
* **Encrypted Notes:** Standard creation, modification, and deletion of notes. Includes custom features like **Pin/Unpin** and **Color-coding**.
* **Secure File Archive:** Importation and viewing of external files (PDF, image, document) using specialized encryption.
* **Biometric Access:** Mandatory login via **Biometric API** (Fingerprint/Face) with a secure **PIN Fallback**.
* **Runtime Security (RASP):** Active device integrity checks and root detection.
* **Configurable Session Timeout:** Automatically locks the application after user-defined inactivity (default 3 minutes), forcing re-authentication.
* **Encrypted Backup:** Exports a user-generated, password-protected archive (`.zip.enc`) for disaster recovery.

---

## 🛡️ Security Architecture & Technology Stack

The project utilizes a multi-layered security approach, known as "Defense in Depth," where the failure of one layer does not compromise the data.

| Layer | Component | Technology Used | Rationale |
| :--- | :--- | :--- | :--- |
| **Data Encryption** | Database Storage | **Room + SQLCipher (AES-256)** | Encrypts the entire SQLite database file (notes) on disk. |
| **File Encryption** | File Storage | **EncryptedFile (Jetpack Security)** | Encrypts binary file contents. |
| **Key Management** | `SecurityDbManager` & `PinManager` | **Android Keystore (TEE)** + **EncryptedSharedPreferences** | Protects the cryptographic keys using hardware binding. |
| **Runtime Defense** | `AuthActivity.isDeviceRooted()` | **RootBeer** | Prevents execution on compromised devices to block memory scraping and system access. |
| **Integrity Check** | `AuthActivity.isAppTampered()` | **Signature Hash Verification** | Prevents the app from running if it has been modified or re-signed by an attacker (Tamper Detection). |
| **Perimeter Defense**| `SecureNotesApplication` | **ProcessLifecycleOwner** + **Handler** | Implements the configurable timeout and the aggressive **cache cleanup** (deletes cleartext files after 10s of background activity) to mitigate data leakage. |
| **Code Structure** | Build Config | **R8 Obfuscation** | Scrambles application logic and class names to deter static reverse engineering. |

---

## 🛠️ Build and Delivery

### Deliverables
* **Final APK:** `SecureNotes.apk` (Signed and Obfuscated).
* **Source Code:** This repository, often delivered as a clean `.zip`.
* **Technical Document:** brief report detailing the implementation choices and security model.

### How to Build & Verification

> **⚠️ IMPORTANT NOTE:** The source code is explicitly configured to verify the **Release** signature hash (Tamper Detection).
>
> If you run the project in **Debug** mode directly from Android Studio (using the default debug key), the integrity check will fail by design, showing an "App Tampered" error.
>
> **To test the fully functional application, please install the provided signed `SecureNotes.apk`.**

1.  **Prerequisites:** Android SDK 34+.
2.  **Obfuscation:** R8 is configured and active for the `release` build type (`minifyEnabled = true`).
3.  **Rebuilding Release:** To rebuild a working Release APK from source, you must generate a new Keystore, determine its SHA-256 signature hash, and update the verification logic in `AuthActivity.java` before signing.

---

## 📜 License

This project is released under the **MIT License**.

```text
Copyright (c) 2025 Vincenzo Barba

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
