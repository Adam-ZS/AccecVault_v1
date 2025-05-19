package com.example.accessvault;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import android.service.autofill.FillContext;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyAutofillService extends AutofillService {

    private static final String TAG = "MyAutofillService";
    private DBHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(getApplicationContext());
        Log.d(TAG, "Autofill Service Created");
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {
        Log.d(TAG, "onFillRequest called");

        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts == null || fillContexts.isEmpty()) {
            callback.onSuccess(null);
            Log.d(TAG, "No fill contexts in request.");
            return;
        }

        // Get the last AssistStructure in the list of contexts
        AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
        String packageName = structure.getActivityComponent().getPackageName();

        AutofillHelper.ParsedStructure parsedStructure = AutofillHelper.parseStructureForFill(structure, packageName);

        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        boolean canProvideFill = false;

        if (parsedStructure.isCompleteForFill()) {
            Credential credential = dbHelper.getCredentialBySiteName(parsedStructure.detectedSiteIdentifier);

            if (credential != null) {
                Log.i(TAG, "Found credential for: " + parsedStructure.detectedSiteIdentifier + ", username: " + credential.getUsername());

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
        }

        if (canProvideFill) {
            callback.onSuccess(responseBuilder.build());
            Log.d(TAG, "onFillRequest: Sent response.");
        } else {
            callback.onSuccess(null);
            Log.d(TAG, "onFillRequest: No valid credentials found.");
        }
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        Log.d(TAG, "onSaveRequest called");

        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts == null || fillContexts.isEmpty()) {
            Log.w(TAG, "No fill contexts provided in SaveRequest.");
            callback.onFailure("No fill contexts for save.");
            return;
        }

        AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
        if (structure == null) {
            Log.e(TAG, "Structure is null in SaveRequest");
            callback.onFailure("Empty assist structure");
            return;
        }

        String packageName = structure.getActivityComponent().getPackageName();
        Credential credentialToSave = AutofillHelper.parseStructureForSave(structure, packageName);

        if (credentialToSave != null &&
                credentialToSave.getSiteName() != null &&
                credentialToSave.getUsername() != null &&
                credentialToSave.getPassword() != null &&
                !credentialToSave.getSiteName().trim().isEmpty() &&
                !credentialToSave.getUsername().trim().isEmpty()) {

            boolean success = dbHelper.addCredential(
                    credentialToSave.getSiteName(),
                    credentialToSave.getUsername(),
                    credentialToSave.getPassword());

            if (success) {
                callback.onSuccess();
                Log.i(TAG, "Credential saved successfully via autofill for: " + credentialToSave.getSiteName());
            } else {
                callback.onFailure("Failed to save to database.");
                Log.e(TAG, "Failed to save credential via autofill for: " + credentialToSave.getSiteName());
            }
        } else {
            callback.onFailure("Could not extract all necessary information to save the credential.");
            Log.w(TAG, "Incomplete data in save request.");
        }
    }

    private String getApplicationName() {
        try {
            return getApplicationContext().getString(R.string.app_name);
        } catch (Exception e) {
            return "AccessVault";
        }
    }
}