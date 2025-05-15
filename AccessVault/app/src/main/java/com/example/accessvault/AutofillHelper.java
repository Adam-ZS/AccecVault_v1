package com.example.accessvault;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.text.InputType;
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
            AssistStructure.ViewNode viewNode = windowNode.getRootViewNode(); // ✅ Correct method
            traverseStructureForFill(viewNode, parsed, packageName);
        }
        return parsed;
    }

    private static void traverseStructureForFill(AssistStructure.ViewNode node, ParsedStructure parsed, String currentPackageName) {
        if (node == null) return;

        AutofillId autofillId = node.getAutofillId();

        if (autofillId != null) {
            parsed.allFields.add(autofillId);

            String[] hints = node.getAutofillHints(); // ✅ From ViewNode
            if (hints != null) {
                for (String hint : hints) {
                    String lowerHint = hint.toLowerCase(Locale.ROOT);
                    if (parsed.usernameId == null && (lowerHint.contains("username") || lowerHint.contains("email"))) {
                        parsed.usernameId = autofillId;
                        Log.d(TAG, "Found username field by hint: " + hint + " ID: " + autofillId);
                    } else if (parsed.passwordId == null && lowerHint.contains("password")) {
                        parsed.passwordId = autofillId;
                        Log.d(TAG, "Found password field by hint: " + hint + " ID: " + autofillId);
                    }
                }
            }

            CharSequence hintText = node.getHint();
            String idName = node.getIdEntry();

            if (parsed.usernameId == null) {
                if (hintText != null && hintText.toString().toLowerCase(Locale.ROOT).contains("user")) {
                    parsed.usernameId = autofillId;
                } else if (idName != null && idName.toLowerCase(Locale.ROOT).contains("username")) {
                    parsed.usernameId = autofillId;
                }
            }
            if (parsed.passwordId == null) {
                if (hintText != null && hintText.toString().toLowerCase(Locale.ROOT).contains("pass")) {
                    parsed.passwordId = autofillId;
                } else if (idName != null && idName.toLowerCase(Locale.ROOT).contains("password")) {
                    parsed.passwordId = autofillId;
                }
            }
        }

        int children = node.getChildCount();
        for (int i = 0; i < children; i++) {
            traverseStructureForFill(node.getChildAt(i), parsed, currentPackageName); // ✅ getChildAt()
        }
    }

    public static Credential parseStructureForSave(AssistStructure structure, String packageName) {
        String username = null;
        String password = null;
        String siteIdentifier = packageName;

        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            AssistStructure.ViewNode root = windowNode.getRootViewNode();
            ExtractedSaveData data = new ExtractedSaveData();
            traverseStructureForSave(root, data);
            if (data.username != null) username = data.username;
            if (data.password != null) password = data.password;
        }

        if (username != null && password != null && siteIdentifier != null) {
            return new Credential(siteIdentifier, username, password);
        }
        return null;
    }

    private static class ExtractedSaveData {
        String username;
        String password;
    }

    private static void traverseStructureForSave(AssistStructure.ViewNode node, ExtractedSaveData data) {
        if (node == null) return;

        AutofillValue value = node.getAutofillValue();
        if (value != null && value.isText()) {
            String textValue = value.getTextValue().toString();
            String[] hints = node.getAutofillHints();

            if (hints != null) {
                for (String hint : hints) {
                    String lowerHint = hint.toLowerCase(Locale.ROOT);
                    if (data.username == null && (lowerHint.contains("username") || lowerHint.contains("email"))) {
                        data.username = textValue;
                        Log.d(TAG, "Save: Found username by hint: " + textValue);
                    } else if (data.password == null && lowerHint.contains("password")) {
                        data.password = textValue;
                        Log.d(TAG, "Save: Found password by hint (value present)");
                    }
                }
            }

            if (data.username == null && node.getHint() != null &&
                    node.getHint().toString().toLowerCase(Locale.ROOT).contains("user")) {
                data.username = textValue;
            }

            if (data.password == null && node.getHint() != null &&
                    node.getHint().toString().toLowerCase(Locale.ROOT).contains("pass")) {
                int inputType = node.getInputType(); // ✅ From ViewNode
                if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        inputType == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                        inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
                    data.password = textValue;
                } else {
                    Log.w(TAG, "Possible password field detected, but input type is not password. Ignoring.");
                }
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseStructureForSave(node.getChildAt(i), data); // ✅ getChildAt()
        }
    }
}