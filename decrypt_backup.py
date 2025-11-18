import os
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes

def decrypt_backup(encrypted_file_path, password, output_zip_path):
    # 1. Read all the crypted file (.enc)
    with open(encrypted_file_path, 'rb') as f:
        data = f.read()

    # 2. Extract Salt and IV 
    # Java: fos.write(salt); (16 byte)
    salt = data[:16]
    # Java: fos.write(iv); (12 byte)
    nonce_iv = data[16:28]
    # crypted payload
    ciphertext = data[28:]

    print(f"Extracted Salt: {salt.hex()}")
    print(f"Extracted IV: {nonce_iv.hex()}")

    # 3. Extract key from passwd (PBKDF2)
    # 10000 iterations, SHA256
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32, # 256 bit key
        salt=salt,
        iterations=10000,
    )
    key = kdf.derive(password.encode('utf-8'))

    # 4. Decrypt using AES-GCM
    aesgcm = AESGCM(key)
    try:
        
        decrypted_data = aesgcm.decrypt(nonce_iv, ciphertext, None)
        
        # 5. Save result (clear Zip)
        with open(output_zip_path, 'wb') as f:
            f.write(decrypted_data)
        
        print(f"SUCCESS! Decrypted File saved in: {output_zip_path}")
        print("Now you can open it like a file Zip.")
        
    except Exception as e:
        print("ERROR: wrong password or corrupted file.")
        print(e)

# change input_file with file name of encrypted backup
input_file = "SecureNotes_Backup_20251118_113945.enc" 
output_file = "unlocked_backup.zip"
user_pass = input("Insert password for backup: ")

decrypt_backup(input_file, user_pass, output_file)