package com.example.accessvault;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class VaultActivity extends AppCompatActivity {
    private ListView listViewCredentials;
    private TextView tvEmptyVault;
    private CredentialsAdapter adapter;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        listViewCredentials = findViewById(R.id.listViewCredentials);
        tvEmptyVault = findViewById(R.id.tvEmptyVault);
        FloatingActionButton btnAddCredential = findViewById(R.id.btnAddCredential);

        DBHelper dbHelper = new DBHelper(this);
        loadCredentials();

        btnAddCredential.setOnClickListener(v -> {
            startActivity(new Intent(VaultActivity.this, AddCredentialActivity.class));
        });

        listViewCredentials.setOnItemClickListener((parent, view, position, id) -> {
            Credential selected = adapter.getItem(position);
            if (selected != null) {
                Toast.makeText(this, "Selected: " + selected.getSiteName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCredentials() {
        List<Credential> credentials = dbHelper.getAllCredentials();
        if (credentials.isEmpty()) {
            tvEmptyVault.setVisibility(View.VISIBLE);
            listViewCredentials.setVisibility(View.GONE);
        } else {
            tvEmptyVault.setVisibility(View.GONE);
            listViewCredentials.setVisibility(View.VISIBLE);
            adapter = new CredentialsAdapter(this, credentials);
            listViewCredentials.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCredentials(); // Refresh vault list
    }
}