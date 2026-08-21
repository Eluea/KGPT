/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.core.ui;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;

import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.instruction.command.Commands;
import tn.eluea.kgpt.text.parse.ParsePattern;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply blur to the Activity window (single blur for all dialogs)
        applyBlurToActivity();

        // Process Intent Extras
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            extras = new Bundle();
        }

        // Initialize ConfigContainer
        ConfigContainer config = new ConfigContainer();
        config.initClient(this);

        // Populate ConfigContainer from extras (if available)
        // This ensures the initial state matches what the requester (e.g. UiInteractor)
        // provided.
        // Even if DialogBox safeguards data from Provider, using the Intent data first
        // is often faster/expected.

        if (extras.containsKey(UiInteractor.EXTRA_CONFIG_SELECTED_MODEL)) {
            try {
                config.selectedModel = LanguageModel
                        .valueOf(extras.getString(UiInteractor.EXTRA_CONFIG_SELECTED_MODEL));
            } catch (Exception ignored) {
            }
        }

        if (extras.containsKey(UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL)) {
            config.languageModelsConfig = extras.getBundle(UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL);
        }

        if (extras.containsKey(UiInteractor.EXTRA_COMMAND_LIST)) {
            String raw = extras.getString(UiInteractor.EXTRA_COMMAND_LIST);
            if (raw != null) {
                config.commands = Commands.decodeCommands(raw);
            }
        }

        if (extras.containsKey(UiInteractor.EXTRA_PATTERN_LIST)) {
            String raw = extras.getString(UiInteractor.EXTRA_PATTERN_LIST);
            if (raw != null) {
                config.patterns = ParsePattern.decode(raw);
            }
        }

        if (extras.containsKey(UiInteractor.EXTRA_OTHER_SETTINGS)) {
            config.otherExtras = extras.getBundle(UiInteractor.EXTRA_OTHER_SETTINGS);
        }

        if (extras.containsKey(UiInteractor.EXTRA_COMMAND_INDEX)) {
            config.focusCommandIndex = extras.getInt(UiInteractor.EXTRA_COMMAND_INDEX, -1);
        }

        // Determine Dialog Type
        DialogType type = DialogType.Settings;
        if (extras.containsKey(UiInteractor.EXTRA_DIALOG_TYPE)) {
            try {
                type = DialogType.valueOf(extras.getString(UiInteractor.EXTRA_DIALOG_TYPE));
            } catch (IllegalArgumentException e) {
                type = DialogType.Settings;
            }
        }

        // Show Dialog
        DialogBoxManager manager = new DialogBoxManager(this, extras, config);
        manager.showDialog(type);
    }
    
    /**
     * Apply blur effect to the Activity window.
     * This creates a single blur layer that persists across all dialogs.
     * If blur is disabled, uses standard dim effect instead.
     * Optimized for performance on low-end devices.
     */
    private void applyBlurToActivity() {
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyBlurToWindow(getWindow(), this);
    }
}
