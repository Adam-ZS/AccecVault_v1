🔒 AccessVault – Biometric Password Manager 

A secure Android password manager that stores credentials using AES encryption , authenticates via fingerprint or face unlock , and supports autofill across apps . 
     

📝 Project Description 

AccessVault is a local, encrypted credential storage app designed to help users securely manage login details without relying on cloud services. It uses the Android Keystore System  to protect the AES key, stores credentials in an SQLite database , and integrates with the Android Autofill Framework  to automatically fill login fields in other apps. 

This app follows modern Android development practices: 

    Material 3 theming
    Secure local-only storage
    Biometric authentication before access
    Encrypted credentials at rest
     

🔑 Features 
🔐 AES Encryption
	
All credentials are encrypted using AES/CBC before saving
🗃️ SQLite Storage
	
Credentials stored locally with unique site names
🧠 Biometric Authentication
	
Uses AndroidX BiometricPrompt API for fingerprint/face unlock
🧩 Android Autofill Support
	
Fills username/password fields in external apps
🌙 Dark Mode Support
	
Automatically adapts to system theme
 
 
🛡️ Security Model 

    Credentials are stored as: iv_base64:encrypted_data_base64
    AES key is generated and protected by the Android Keystore 
    Decryption only happens after successful biometric authentication
    No raw passwords are ever exposed in logs or UI unless decrypted
    Data is never sent to a server — it stays 100% local 
     

📦 Technologies Used 
Android Keystore
	
Secure AES key generation
BiometricPrompt API
	
Biometric authentication support
SQLiteOpenHelper
	
Local credential storage
Base64 Encoding
	
For safe binary data encoding
Cipher
	
AES encryption/decryption
Material 3 (DayNight)
	
Light/dark mode theming
AutofillService
	
Auto-fills credentials in external apps
 
 
💻 Sample Code Highlights 
🔒 AES Encryption Using Android Keystore (DBHelper.java) 

private SecretKey getOrCreateSecretKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
    keyStore.load(null);

    if (!keyStore.containsAlias(KEY_ALIAS)) {
        KeyGenerator kg = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

        KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setRandomizedEncryptionRequired(true)
                .build();

        kg.init(keyGenSpec);
        return kg.generateKey();
    }

    return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
}
 
 
🧠 Biometric Authentication (LoginActivity.java) 

promptInfo = new BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock AccessVault")
        .setSubtitle("Use fingerprint or face unlock")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build();

checkBiometricSupportAndAuthenticate();
 
 
🧩 Autofill Integration (MyAutofillService.java) 


@Override
public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {
    List<AssistStructure> structures = request.getStructures();
    if (structures == null || structures.isEmpty()) {
        callback.onSuccess(null);
        return;
    }

    AssistStructure structure = structures.get(0);
    String packageName = structure.getActivityComponent().getPackageName();

    Credential credential = dbHelper.getCredentialBySiteName(packageName);
    if (credential != null) {
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
        String presentationText = credential.getUsername() + " (" + getApplicationName() + ")";
        presentation.setTextViewText(android.R.id.text1, presentationText);

        Dataset dataset = new Dataset.Builder(presentation)
                .setValue(parsedStructure.usernameId, AutofillValue.forText(credential.getUsername()))
                .setValue(parsedStructure.passwordId, AutofillValue.forText(credential.getPassword()))
                .build();

        responseBuilder.addDataset(dataset);
        canProvideFill = true;
    }

    if (canProvideFill) {
        callback.onSuccess(responseBuilder.build());
    } else {
        callback.onSuccess(null);
    }
}
 
 
 

Open in Android Studio  

Build and run on: 

    Physical device (recommended)
    Emulator with API level 26+ (for Autofill and Biometric support)
     

Enable the app in: 
 
    Settings > Security & Location > Autofill service
     
     
     


 
📄 License 

MIT License – see LICENSE  
🚀 Future Enhancements 

    ✅ Add export/import feature (with optional encryption)
    ✅ Allow multiple credentials per site
    ✅ Implement GCM instead of CBC for better AES support
    ✅ Add password generator and strength meter
    ✅ Use Room Persistence Library for cleaner architecture
     
