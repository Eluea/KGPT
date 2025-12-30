package tn.eluea.kgpt.external.dialog.box;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;

public class PatternListDialogBox extends DialogBox {
    public PatternListDialogBox(DialogBoxManager dialogManager, Activity parent,
                                Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardPatterns();

        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_list, null);
        LinearLayout itemsContainer = layout.findViewById(R.id.items_container);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        ImageView ivIcon = layout.findViewById(R.id.iv_icon);
        TextView tvEmpty = layout.findViewById(R.id.tv_empty);
        MaterialButton btnBack = layout.findViewById(R.id.btn_back);
        MaterialButton btnNew = layout.findViewById(R.id.btn_new);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        
        tvTitle.setText("Trigger Symbols");
        ivIcon.setImageResource(R.drawable.ic_document_filled);
        ivIcon.setColorFilter(iconColor);
        btnNew.setVisibility(View.GONE);
        
        if (getConfig().patterns.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No patterns");
        } else {
            for (int i = 0; i < getConfig().patterns.size(); i++) {
                ParsePattern pattern = getConfig().patterns.get(i);
                View itemView = getParent().getLayoutInflater().inflate(R.layout.item_list_option, itemsContainer, false);
                
                TextView tvName = itemView.findViewById(R.id.tv_item_name);
                TextView tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
                ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
                ImageView arrowIcon = itemView.findViewById(R.id.iv_arrow);
                
                // Show enabled/disabled status in title
                String title = pattern.getType().title;
                if (!pattern.isEnabled()) {
                    title += " (Disabled)";
                }
                tvName.setText(title);
                
                // Show user-friendly symbol instead of regex
                String symbol = getDisplaySymbol(pattern);
                if (tvSubtitle != null) {
                    tvSubtitle.setText("Trigger: " + symbol);
                    tvSubtitle.setVisibility(View.VISIBLE);
                }
                
                itemIcon.setImageResource(R.drawable.ic_document_filled);
                itemIcon.setColorFilter(iconColor);
                if (arrowIcon != null) arrowIcon.setColorFilter(iconColor);
                
                // Set alpha based on enabled state
                itemView.setAlpha(pattern.isEnabled() ? 1.0f : 0.5f);
                
                // Allow editing for all editable patterns
                if (pattern.getType().editable) {
                    final int index = i;
                    itemView.setOnClickListener(v -> {
                        getConfig().focusPatternIndex = index;
                        switchToDialog(DialogType.EditPattern);
                    });
                }
                
                itemsContainer.addView(itemView);
            }
        }
        
        btnBack.setOnClickListener(v -> switchToDialog(DialogType.Settings));

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
        
        return dialog;
    }
    
    /**
     * Extract and display user-friendly symbol from pattern
     */
    private String getDisplaySymbol(ParsePattern pattern) {
        String regex = pattern.getPattern().pattern();
        String symbol = PatternType.regexToSymbol(regex);
        
        if (symbol != null && !symbol.isEmpty()) {
            return "\"" + symbol + "\"";
        }
        
        // Fallback to default symbol
        return "\"" + pattern.getType().defaultSymbol + "\"";
    }
}
