package com.example.accessvault;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends AppCompatActivity {

    private ListView listViewCredentials;
    private FloatingActionButton btnAddCredential;
    private DBHelper dbHelper;
    private CredentialsAdapter adapter;
    private List<Credential> credentialsList;
    private TextView tvEmptyVault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        listViewCredentials = findViewById(R.id.listViewCredentials);
        btnAddCredential = findViewById(R.id.btnAddCredential);
        tvEmptyVault = findViewById(R.id.tvEmptyVault);

        dbHelper = new DBHelper(this);
        credentialsList = new ArrayList<>();

        adapter = new CredentialsAdapter(this, credentialsList);
        listViewCredentials.setAdapter(adapter);

        btnAddCredential.setOnClickListener(v -> {
            startActivity(new Intent(VaultActivity.this, AddCredentialActivity.class));
        });

        listViewCredentials.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteConfirmationDialog(credentialsList.get(position));
            return true;
        });

        listViewCredentials.setOnItemClickListener((parent, view, position, id) -> {
            Credential selected = credentialsList.get(position);
            Toast.makeText(VaultActivity.this,
                    "Site: " + selected.getSiteName() + "\nUser: " + selected.getUsername(),
                    Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCredentials();
    }

    private void loadCredentials() {
        try {
            List<Credential> newCredentials = dbHelper.getAllCredentials();
            credentialsList.clear();
            if (newCredentials != null) {
                credentialsList.addAll(newCredentials);
            }

            if (credentialsList.isEmpty()) {
                tvEmptyVault.setVisibility(View.VISIBLE);
                listViewCredentials.setVisibility(View.GONE);
            } else {
                tvEmptyVault.setVisibility(View.GONE);
                listViewCredentials.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading credentials: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showDeleteConfirmationDialog(final Credential credential) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Credential")
                .setMessage("Are you sure you want to delete the credential for " + credential.getSiteName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteCredential(credential.getId());
                    Toast.makeText(VaultActivity.this, "Credential deleted.", Toast.LENGTH_SHORT).show();
                    loadCredentials();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}