/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * This file is part of KGPT.
 * Based on original code from KeyboardGPT by Mino260806.
 * Original: https://github.com/Mino260806/KeyboardGPT
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
        android.view.Window window = getWindow();
        if (window == null) return;
        
        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_gpt_ui", MODE_PRIVATE);
        boolean isBlurEnabled = prefs.getBoolean("blur_enabled", true);
        int blurIntensity = prefs.getInt("blur_intensity", 25);
        int blurTintColor = prefs.getInt("blur_tint_color", android.graphics.Color.TRANSPARENT);
        
        android.view.WindowManager.LayoutParams params = window.getAttributes();
        
        // Check if device is low-end to reduce blur intensity
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isLowRamDevice = am != null && am.isLowRamDevice();
        
        // Reduce max blur radius for better performance
        // Max 30 instead of 50, and further reduced on low-end devices
        int maxBlurRadius = isLowRamDevice ? 15 : 30;
        
        if (isBlurEnabled && blurIntensity > 0) {
            // Blur is enabled
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                int blurRadius = Math.min((blurIntensity * maxBlurRadius) / 100, maxBlurRadius);
                params.setBlurBehindRadius(blurRadius);
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            }
            
            // Apply tint color if set, otherwise use dim
            if (blurTintColor != android.graphics.Color.TRANSPARENT) {
                int alpha = 120;
                int colorWithAlpha = android.graphics.Color.argb(alpha, 
                    android.graphics.Color.red(blurTintColor), 
                    android.graphics.Color.green(blurTintColor),
                    android.graphics.Color.blue(blurTintColor));
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(colorWithAlpha));
                params.dimAmount = 0f;
            } else {
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                params.dimAmount = 0.5f;
            }
        } else {
            // Blur is disabled - use standard dim effect
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                params.setBlurBehindRadius(0);
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            }
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            params.dimAmount = 0.5f;
        }
        
        window.setAttributes(params);
    }
}
