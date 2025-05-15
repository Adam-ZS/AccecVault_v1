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
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_credential, parent, false);
        }

        Credential currentCredential = getItem(position);
        TextView tvSiteName = listItemView.findViewById(R.id.tvSiteNameItem);
        TextView tvUsername = listItemView.findViewById(R.id.tvUsernameItem);

        if (currentCredential != null) {
            tvSiteName.setText(currentCredential.getSiteName());
            tvUsername.setText(currentCredential.getUsername());
        }

        return listItemView;
    }
}