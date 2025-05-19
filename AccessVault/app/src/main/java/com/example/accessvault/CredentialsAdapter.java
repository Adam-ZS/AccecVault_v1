package com.example.accessvault;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CredentialsAdapter extends ArrayAdapter<Credential> {
    public CredentialsAdapter(@NonNull Context context, @NonNull List<Credential> credentials) {
        super(context, 0, credentials);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Credential credential = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
        }

        TextView text1 = convertView.findViewById(android.R.id.text1);
        text1.setText(credential != null ? credential.getSiteName() : "Unknown Site");

        return convertView;
    }
}