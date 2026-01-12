/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 */
package tn.eluea.kgpt.backup;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class BackupWarningDialog {

    public interface OnConfirmListener {
        void onConfirmed();
    }

    private final Context context;
    private final OnConfirmListener listener;

    public BackupWarningDialog(Context context, OnConfirmListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_backup_warning, null);

        // Apply theme
        BottomSheetHelper.applyTheme(context, sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(context);
        dialog.setContentView(sheetView);

        TextInputEditText etConfirmation = sheetView.findViewById(R.id.et_confirmation);
        Button btnConfirm = sheetView.findViewById(R.id.btn_confirm);
        Button btnCancel = sheetView.findViewById(R.id.btn_cancel);

        // Strict input filter: Only allow characters that match "YES" in sequence
        InputFilter strictFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Construct the resulting text if this change is applied
                StringBuilder builder = new StringBuilder(dest);
                builder.replace(dstart, dend, source.subSequence(start, end).toString());
                String result = builder.toString().toUpperCase();

                // Check if the result is a prefix of "YES"
                String target = "YES";
                if (result.length() > target.length()) {
                    return ""; // Too long
                }

                if (target.startsWith(result)) {
                    return null; // Accept (convert to uppercase is handled by inputType but we force it here
                                 // logic-wise)
                }

                return ""; // Reject
            }
        };

        // Also force uppercase input filter just in case
        InputFilter allCaps = new InputFilter.AllCaps();

        etConfirmation.setFilters(new InputFilter[] { allCaps, strictFilter });

        etConfirmation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().toUpperCase();
                btnConfirm.setEnabled("YES".equals(text));

                // Double check enforcement (redundant with InputFilter but safe)
                if (!"YES".startsWith(text) && !text.isEmpty()) {
                    // This theoretically shouldn't happen with the filter,
                    // but if it does, clear invalid input
                    etConfirmation.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onConfirmed();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
