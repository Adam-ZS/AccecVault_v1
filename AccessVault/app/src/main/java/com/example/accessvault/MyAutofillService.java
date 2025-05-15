package com.example.accessvault;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;
// import java.util.Objects; // Not strictly needed for the completed code below based on usage

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyAutofillService extends AutofillService {

    private static final String TAG = "MyAutofillService";
    private DBHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(this);
        Log.d(TAG, "Autofill Service Created");
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {
        Log.d(TAG, "onFillRequest called");
        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts.isEmpty()) {
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
        boolean canProvideSave = false;

        // 1. Try to provide fill data
        if (parsedStructure.isCompleteForFill()) {
            Log.d(TAG, "Structure is complete for fill. UsernameId: " + parsedStructure.usernameId + ", PasswordId: " + parsedStructure.passwordId + ", Site: " + parsedStructure.detectedSiteIdentifier);
            Credential credential = dbHelper.getCredentialBySiteName(parsedStructure.detectedSiteIdentifier);

            if (credential != null) {
                Log.i(TAG, "Found credential for: " + parsedStructure.detectedSiteIdentifier + " with username: " + credential.getUsername());
                // Create a presentation for the dataset suggestion.
                RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
                String presentationText = credential.getUsername() + " (" + getString(R.string.app_name) + ")";
                presentation.setTextViewText(android.R.id.text1, presentationText);

                Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
                Dataset.Builder builder;
                builder = datasetBuilder.setValue(parsedStructure.usernameId, AutofillValue.forText(credential.getUsername()));
                datasetBuilder.setValue(parsedStructure.passwordId, AutofillValue.forText(credential.getPassword()));
                // Optionally, you can authenticate the dataset if the key requires user auth for each use
                // datasetBuilder.setAuthentication(createAuthIntentSender());

                responseBuilder.addDataset(datasetBuilder.build());
                canProvideFill = true;
            } else {
                Log.d(TAG, "No credential found in database for: " + parsedStructure.detectedSiteIdentifier);
            }
        } else {
            Log.d(TAG, "Structure is not complete for fill. UsernameId: " + parsedStructure.usernameId + ", PasswordId: " + parsedStructure.passwordId + ", Site: " + parsedStructure.detectedSiteIdentifier);
        }

        // 2. Offer to save new credentials if the user enters them
        if (parsedStructure.isCompleteForSave()) { // Checks usernameId, passwordId, and detectedSiteIdentifier
            Log.d(TAG, "Structure is complete for save. Offering save capability for site: " + parsedStructure.detectedSiteIdentifier);
            AutofillId[] requiredSaveIds = {parsedStructure.usernameId, parsedStructure.passwordId};

            SaveInfo.Builder saveInfoBuilder = new SaveInfo.Builder(
                    SaveInfo.SAVE_DATA_TYPE_USERNAME | SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                    requiredSaveIds
            );
            // Allows the user to decline saving the credentials.
            saveInfoBuilder.setNegativeAction(SaveInfo.NEGATIVE_BUTTON_STYLE_CANCEL, null);
            // Optional: Set a description for the save UI.
            // saveInfoBuilder.setDescription("Save to " + getString(R.string.app_name) + "?");
            // You can also use custom descriptions. For now, the default or simple text is fine.
            // saveInfoBuilder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE); // Example flag

            responseBuilder.setSaveInfo(saveInfoBuilder.build());
            canProvideSave = true;
        } else {
            Log.d(TAG, "Structure is not complete for save. Cannot offer save. UsernameId: " + parsedStructure.usernameId +
                    ", PasswordId: " + parsedStructure.passwordId +
                    ", SiteIdentifier: " + parsedStructure.detectedSiteIdentifier);
        }

        if (canProvideFill || canProvideSave) {
            callback.onSuccess(responseBuilder.build());
            Log.d(TAG, "onFillRequest: Sent response. CanFill: " + canProvideFill + ", CanSave: " + canProvideSave);
        } else {
            // If we cannot fill (no matching credential or structure not suitable for fill)
            // AND we cannot save (structure not suitable for save), then send null.
            callback.onSuccess(null);
            Log.d(TAG, "onFillRequest: Sent null response as cannot fill and cannot save.");
        }
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        Log.d(TAG, "onSaveRequest called");
        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts.isEmpty()) {
            Log.w(TAG, "No fill contexts provided in SaveRequest.");
            callback.onFailure("No fill contexts for save.");
            return;
        }

        AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
        String packageName = structure.getActivityComponent().getPackageName();

        // Use AutofillHelper to parse the structure and extract the values entered by the user.
        Credential credentialToSave = AutofillHelper.parseStructureForSave(structure, packageName);

        if (credentialToSave != null && credentialToSave.getSiteName() != null &&
                credentialToSave.getUsername() != null && credentialToSave.getPassword() != null &&
                !credentialToSave.getSiteName().trim().isEmpty() &&
                !credentialToSave.getUsername().trim().isEmpty() /* Password can be empty intentionally */) {

            Log.d(TAG, "Attempting to save credential for site: " + credentialToSave.getSiteName() +
                    ", user: " + credentialToSave.getUsername());

            boolean success = dbHelper.addCredential(
                    credentialToSave.getSiteName(),
                    credentialToSave.getUsername(),
                    credentialToSave.getPassword()
            );

            if (success) {
                Log.i(TAG, "Credential saved successfully via autofill for: " + credentialToSave.getSiteName());
                callback.onSuccess();
            } else {
                Log.e(TAG, "Failed to save credential via autofill for: " + credentialToSave.getSiteName());
                // Provide a more specific failure message if dbHelper returns more info
                callback.onFailure("Failed to save to database. Site may already exist with different data or DB error.");
            }
        } else {
            Log.w(TAG, "Could not parse all required credential information from save structure, or critical info missing.");
            if (credentialToSave != null) {
                Log.w(TAG, "Parsed Data - Site: " + credentialToSave.getSiteName() +
                        ", User: " + credentialToSave.getUsername() +
                        ", Pass is null: " + (credentialToSave.getPassword() == null));
            }
            callback.onFailure("Could not extract all necessary information to save the credential.");
        }
    }

    @Override
    public void onConnected() {
        super.onConnected();
        Log.d(TAG, "onConnected: Autofill Service is connected.");
        // Perform any setup that needs to happen when the service is bound.
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        Log.d(TAG, "onDisconnected: Autofill Service is disconnected.");
        // Perform any cleanup.
    }
}