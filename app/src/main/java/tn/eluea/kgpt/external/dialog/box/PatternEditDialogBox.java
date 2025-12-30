package tn.eluea.kgpt.external.dialog.box;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.regex.Pattern;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;

public class PatternEditDialogBox extends DialogBox {
    public PatternEditDialogBox(DialogBoxManager dialogManager, Activity parent,
                                Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardPatterns();

        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_pattern_edit, null);
        
        TextInputEditText symbolEditText = layout.findViewById(R.id.edit_symbol);
        TextInputLayout inputLayout = layout.findViewById(R.id.input_layout_symbol);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        TextView tvDescription = layout.findViewById(R.id.tv_description);
        TextView tvExample = layout.findViewById(R.id.tv_example);
        com.google.android.material.materialswitch.MaterialSwitch switchEnabled = layout.findViewById(R.id.switch_enabled);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_save);
        MaterialButton btnReset = layout.findViewById(R.id.btn_reset);
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        if (headerIcon != null) headerIcon.setColorFilter(iconColor);

        final int patternIndex = getConfig().focusPatternIndex;
        ParsePattern pattern = getConfig().patterns.get(patternIndex);
        
        tvTitle.setText("Edit " + pattern.getType().title);
        tvDescription.setText(pattern.getType().description);
        
        // Set enabled state
        switchEnabled.setChecked(pattern.isEnabled());
        
        // Extract current symbol from regex
        String currentSymbol = PatternType.regexToSymbol(pattern.getPattern().pattern());
        if (currentSymbol == null || currentSymbol.isEmpty()) {
            currentSymbol = pattern.getType().defaultSymbol;
        }
        symbolEditText.setText(currentSymbol);
        
        // Update example
        updateExample(tvExample, currentSymbol, pattern.getType());

        if (!pattern.getType().editable) {
            symbolEditText.setEnabled(false);
            inputLayout.setEnabled(false);
            btnSave.setEnabled(false);
            btnReset.setVisibility(View.GONE);
            switchEnabled.setEnabled(false);
        } else {
            // Listen for symbol changes
            symbolEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateExample(tvExample, s.toString(), pattern.getType());
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
        
        btnReset.setOnClickListener(v -> {
            symbolEditText.setText(pattern.getType().defaultSymbol);
        });
        
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            switchToDialog(DialogType.EditPatternList);
        });
        
        btnSave.setOnClickListener(v -> {
            String symbol = symbolEditText.getText().toString();
            boolean isEnabled = switchEnabled.isChecked();
            
            if (symbol.isEmpty()) {
                Toast.makeText(getContext(), "Symbol cannot be empty", Toast.LENGTH_LONG).show();
                return;
            }

            // Forbidden symbols
            if (List.of("]", "[", "-", " ", "\n", "\t").contains(symbol)) {
                Toast.makeText(getContext(), "This symbol is not allowed", Toast.LENGTH_LONG).show();
                return;
            }

            // Convert symbol to regex
            String newRegex = PatternType.symbolToRegex(symbol, pattern.getType().groupCount);
            if (newRegex == null) {
                Toast.makeText(getContext(), "Could not create pattern for this symbol", Toast.LENGTH_LONG).show();
                return;
            }

            // Validate regex
            try {
                Pattern.compile(newRegex);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid pattern generated", Toast.LENGTH_LONG).show();
                return;
            }

            // Check for duplicates
            long similarCount = getConfig().patterns.stream()
                    .filter((c) -> c.getPattern().pattern().equals(newRegex)).count();
            if (similarCount >= 1) {
                Toast.makeText(getContext(), "This symbol is already used by another pattern", Toast.LENGTH_LONG).show();
                return;
            }

            // Create new pattern with enabled state
            ParsePattern newPattern = new ParsePattern(pattern.getType(), newRegex, pattern.getExtras());
            newPattern.setEnabled(isEnabled);
            
            getConfig().patterns.remove(patternIndex);
            getConfig().patterns.add(patternIndex, newPattern);

            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();
            
            // Send broadcast to notify TextParser of the change
            android.content.Intent broadcastIntent = new android.content.Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_PATTERN_LIST, 
                tn.eluea.kgpt.text.parse.ParsePattern.encode(getConfig().patterns));
            getContext().sendBroadcast(broadcastIntent);

            String statusMsg = isEnabled ? "enabled" : "disabled";
            Toast.makeText(getContext(), "Trigger \"" + symbol + "\" " + statusMsg, Toast.LENGTH_SHORT).show();
            
            // Go back to pattern list instead of closing
            switchToDialog(DialogType.EditPatternList);
        });

        return dialog;
    }
    
    private void updateExample(TextView tvExample, String symbol, PatternType type) {
        if (symbol == null || symbol.isEmpty()) {
            symbol = type.defaultSymbol;
        }
        
        String example;
        switch (type) {
            case Settings:
                example = "\"" + symbol + "\" → Opens settings";
                break;
            case CommandAI:
                example = "\"Hello, how are you?" + symbol + "\" → AI responds";
                break;
            case CommandCustom:
                example = "\"Hello" + symbol + "translate" + symbol + "\" → Translates";
                break;
            case FormatItalic:
                example = "\"text" + symbol + "\" → italic text";
                break;
            case FormatBold:
                example = "\"text" + symbol + "\" → bold text";
                break;
            case FormatCrossout:
                example = "\"text" + symbol + "\" → strikethrough";
                break;
            case FormatUnderline:
                example = "\"text" + symbol + "\" → underlined";
                break;
            default:
                example = "Type text, then add \"" + symbol + "\" at the end";
        }
        tvExample.setText(example);
    }
}
