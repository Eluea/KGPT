/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.core.ui.dialog.box;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.CompoundButtonCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.model.CustomProvider;
import tn.eluea.kgpt.llm.model.CustomProviderManager;
import tn.eluea.kgpt.util.ProviderLogoHelper;

/**
 * Model chooser shown from the floating settings sheet (€ trigger).
 *
 * Shows every built-in provider with its real logo (ProviderLogoHelper),
 * then the user's saved Custom Providers, then an "Add Custom Provider"
 * entry that opens the in-app editor. Selecting a custom provider persists
 * it via CustomProviderManager and broadcasts the same DIALOG_RESULT extras
 * used by the in-app flow so hooked keyboards switch live.
 */
public class ChoseModelDialogBox extends DialogBox {

    private static final String TAG = "KGPT_ChooseModel";

    private static final String EXTRA_SELECTED_PROVIDER_TYPE = "tn.eluea.kgpt.config.SELECTED_PROVIDER_TYPE";
    private static final String EXTRA_SELECTED_CUSTOM_PROVIDER_ID = "tn.eluea.kgpt.config.SELECTED_CUSTOM_PROVIDER_ID";

    public ChoseModelDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        try {
            return buildInternal();
        } catch (Throwable t) {
            android.util.Log.e(TAG, "ChoseModel build failed", t);
            // Never leave the sheet blank: minimal built-in list fallback
            try {
                tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet =
                        new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());
                View layout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_choose_model, null);
                LinearLayout container = layout.findViewById(R.id.models_container);
                for (LanguageModel m : LanguageModel.values()) {
                    View row = addModelRow(container, getContext(), m.label,
                            ProviderLogoHelper.getLogoRes(m), false);
                    row.setOnClickListener(v -> {
                        getConfig().selectedModel = m;
                        sheet.dismiss();
                        switchToDialog(DialogType.ConfigureModel);
                    });
                    container.addView(row);
                }
                sheet.setContentView(layout);
                return sheet;
            } catch (Throwable fatal) {
                android.util.Log.e(TAG, "Fallback build also failed", fatal);
                return null;
            }
        }
    }

    private Dialog buildInternal() {
        safeguardModelData();

        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                getContext());
        android.content.Context themedContext = sheet.getContext();

        View layout = LayoutInflater.from(themedContext).inflate(R.layout.dialog_choose_model,
                null);

        LinearLayout modelsContainer = layout.findViewById(R.id.models_container);

        // Header Icon Tint
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        View headerIconContainer = layout.findViewById(R.id.icon_container);
        if (headerIcon != null) {
            tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext,
                    headerIcon, headerIconContainer);
        }

        CustomProviderManager manager = CustomProviderManager.getInstance();
        boolean customSelected = manager.isCustomProviderSelected();
        String selectedCustomId = customSelected ? manager.getSelectedCustomProviderId() : null;
        LanguageModel builtinSelected = customSelected ? null : getConfig().selectedModel;

        // ---------- Built-in providers ----------
        LanguageModel[] models = LanguageModel.values();
        for (LanguageModel model : models) {
            boolean isSelected = !customSelected && model == builtinSelected;
            View itemView = addModelRow(modelsContainer, themedContext,
                    model.label, ProviderLogoHelper.getLogoRes(model), isSelected);

            View.OnClickListener select = v -> {
                getConfig().selectedModel = model;
                sheet.dismiss();
                switchToDialog(DialogType.ConfigureModel);
            };
            itemView.setOnClickListener(select);
            CheckBox cb = itemView.findViewById(R.id.cb_selected);
            if (cb != null) {
                cb.setOnClickListener(v -> {
                    getConfig().selectedModel = model;
                    sheet.dismiss();
                    switchToDialog(DialogType.ConfigureModel);
                });
            }

            modelsContainer.addView(itemView);
        }

        // ---------- Custom providers ----------
        java.util.List<CustomProvider> customs = manager.getCustomProviders();
        if (customs != null && !customs.isEmpty()) {
            addSectionLabel(modelsContainer, themedContext,
                    themedContext.getString(R.string.badge_custom_provider));

            for (CustomProvider cp : customs) {
                boolean isSelected = customSelected && cp.getId() != null
                        && cp.getId().equals(selectedCustomId);
                View itemView = addModelRow(modelsContainer, themedContext,
                        cp.getName(), ProviderLogoHelper.getCustomProviderLogoRes(), isSelected);

                View.OnClickListener select = v -> {
                    selectCustomProvider(cp.getId());
                    sheet.dismiss();
                };
                itemView.setOnClickListener(select);
                CheckBox cb = itemView.findViewById(R.id.cb_selected);
                if (cb != null) cb.setOnClickListener(select);
                modelsContainer.addView(itemView);
            }
        }

        // ---------- Add Custom Provider ----------
        View addView = addModelRow(modelsContainer, themedContext,
                themedContext.getString(R.string.btn_add_custom_provider),
                R.drawable.ic_provider_custom, false);
        addView.setOnClickListener(v -> {
            // Open the editor directly in this floating flow (no app redirect)
            sheet.dismiss();
            switchToDialog(DialogType.AddCustomProvider);
        });
        modelsContainer.addView(addView);

        // Back button
        View btnBack = layout.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                sheet.dismiss();
                switchToDialog(DialogType.Settings);
            });
        }

        sheet.setContentView(layout);
        return sheet;
    }

    /** Inflate one option row and apply theme tints; caller adds listeners. */
    private View addModelRow(LinearLayout container, android.content.Context themedContext,
            String name, int iconRes, boolean checked) {
        View itemView = LayoutInflater.from(themedContext).inflate(
                R.layout.item_model_option, container, false);

        TextView tvName = itemView.findViewById(R.id.tv_model_name);
        CheckBox checkBox = itemView.findViewById(R.id.cb_selected);
        ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
        View itemIconContainer = itemView.findViewById(R.id.icon_container);

        if (tvName != null) tvName.setText(name);
        if (checkBox != null) checkBox.setChecked(checked);
        if (itemIcon != null) itemIcon.setImageResource(iconRes);

        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext,
                itemIcon, itemIconContainer);
        if (checkBox != null) {
            CompoundButtonCompat.setButtonTintList(checkBox,
                    tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils
                            .getCheckboxColorStateList(themedContext));
        }
        return itemView;
    }

    private void addSectionLabel(LinearLayout container, android.content.Context ctx, String text) {
        TextView label = new TextView(ctx);
        label.setText(text);
        label.setAllCaps(true);
        label.setTextSize(12f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        int pad = Math.round(8 * ctx.getResources().getDisplayMetrics().density);
        label.setPadding(pad * 2, pad * 2, pad, pad);
        container.addView(label);
    }

    /**
     * Persist a custom-provider selection and broadcast the same extras the
     * in-app flow uses, so hooked keyboards switch immediately.
     */
    private void selectCustomProvider(String providerId) {
        CustomProviderManager manager = CustomProviderManager.getInstance();
        manager.setSelectedProviderType(CustomProviderManager.TYPE_CUSTOM);
        manager.setSelectedCustomProviderId(providerId);

        try {
            Intent result = new Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            result.putExtra(EXTRA_SELECTED_PROVIDER_TYPE, CustomProviderManager.TYPE_CUSTOM);
            result.putExtra(EXTRA_SELECTED_CUSTOM_PROVIDER_ID, providerId);
            getContext().sendBroadcast(result);
        } catch (Throwable ignored) {}
    }
}
