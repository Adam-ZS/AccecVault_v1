package com.example.accessvault;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddCredentialActivity extends AppCompatActivity {
    private EditText etSiteName, etUsername, etPassword;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credential);

        etSiteName = findViewById(R.id.etSiteName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnSave = findViewById(R.id.btnSaveCredential);

        dbHelper = new DBHelper(this);

        btnSave.setOnClickListener(v -> {
            String siteName = etSiteName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (siteName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = dbHelper.addCredential(siteName, username, password);
            if (success) {
                Toast.makeText(this, "Credential saved successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to VaultActivity
            } else {
                Toast.makeText(this, "Failed to save credential", Toast.LENGTH_LONG).show();
            }
        });
    }
}