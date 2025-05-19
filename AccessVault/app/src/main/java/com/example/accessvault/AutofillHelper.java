package com.example.accessvault;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillHelper {
    private static final String TAG = "AutofillHelper";

    public static class ParsedStructure {
        public AutofillId usernameId;
        public AutofillId passwordId;
        public String detectedSiteIdentifier;
        public List<AutofillId> allFields = new ArrayList<>();

        public boolean isCompleteForFill() {
            return usernameId != null && passwordId != null;
        }

        public boolean isCompleteForSave() {
            return usernameId != null && passwordId != null && detectedSiteIdentifier != null;
        }
    }

    public static ParsedStructure parseStructureForFill(AssistStructure structure, String packageName) {
        ParsedStructure parsed = new ParsedStructure();
        parsed.detectedSiteIdentifier = packageName;

        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            traverseStructureForFill(windowNode.getRootViewNode(), parsed, packageName);
        }
        return parsed;
    }

    private static void traverseStructureForFill(AssistStructure.ViewNode node, ParsedStructure parsed, String currentPackageName) {
        if (node == null) return;

        AutofillId autofillId = node.getAutofillId();
        if (autofillId == null) return;

        parsed.allFields.add(autofillId);

        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint : hints) {
                if (parsed.usernameId == null && (hint.toLowerCase(Locale.ROOT).contains("username") ||
                        hint.toLowerCase(Locale.ROOT).contains("email"))) {
                    parsed.usernameId = autofillId;
                } else if (parsed.passwordId == null && hint.toLowerCase(Locale.ROOT).contains("password")) {
                    parsed.passwordId = autofillId;
                }
            }
        }

        CharSequence hintText = node.getHint();
        String idName = node.getIdEntry();

        if (parsed.usernameId == null && hintText != null &&
                hintText.toString().toLowerCase(Locale.ROOT).contains("user")) {
            parsed.usernameId = autofillId;
        }

        if (parsed.passwordId == null && hintText != null &&
                hintText.toString().toLowerCase(Locale.ROOT).contains("pass")) {
            int inputType = node.getInputType();
            if (inputType == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    inputType == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    inputType == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                    inputType == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
                parsed.passwordId = autofillId;
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseStructureForFill(node.getChildAt(i), parsed, currentPackageName);
        }
    }

    public static Credential parseStructureForSave(AssistStructure structure, String packageName) {
        String username = null;
        String password = null;
        String siteIdentifier = packageName;

        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            Credential credential = new Credential(siteIdentifier, username, password);
            traverseStructureForSave(windowNode.getRootViewNode(), credential);
            if (credential.getUsername() != null && credential.getPassword() != null) {
                credential.setSiteName(siteIdentifier);
                return credential;
            }
        }
        return null;
    }

    private static void traverseStructureForSave(AssistStructure.ViewNode node, Credential credential) {
        if (node == null) return;

        AutofillValue value = node.getAutofillValue();
        if (value != null && value.isText()) {
            String textValue = value.getTextValue().toString();
            String[] hints = node.getAutofillHints();

            if (hints != null) {
                for (String hint : hints) {
                    String lowerHint = hint.toLowerCase(Locale.ROOT);
                    if (credential.getUsername() == null && (lowerHint.contains("username") || lowerHint.contains("email"))) {
                        credential.setUsername(textValue);
                    } else if (credential.getPassword() == null && lowerHint.contains("password")) {
                        credential.setPassword(textValue);
                    }
                }
            }

            if (credential.getUsername() == null && node.getHint() != null &&
                    node.getHint().toString().toLowerCase(Locale.ROOT).contains("user")) {
                credential.setUsername(value.getTextValue().toString());
            }

            if (credential.getPassword() == null && node.getHint() != null &&
                    node.getHint().toString().toLowerCase(Locale.ROOT).contains("pass")) {
                credential.setPassword(value.getTextValue().toString());
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseStructureForSave(node.getChildAt(i), credential);
        }
    }
}