/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.ui.settings;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class BlurControlActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "keyboard_gpt_ui";
    private static final String PREF_BLUR_ENABLED = "blur_enabled";
    private static final String PREF_MATERIAL_YOU_BLUR = "material_you_blur";
    private static final String PREF_BLUR_INTENSITY = "blur_intensity";
    private static final String PREF_BLUR_TINT_COLOR = "blur_tint_color";

    private SharedPreferences prefs;
    private MaterialSwitch switchBlurEnabled;
    private MaterialSwitch switchMaterialYouBlur;
    private LinearLayout customizationContainer;
    private LinearLayout manualSettingsContainer;
    private Slider sliderIntensity;
    private TextView tvIntensityValue;
    private View colorPreview;
    private MaterialButton btnResetSettings;
    private TextView tvCardColorPickerTitle;
    private MaterialCardView cardColorPicker;

    private int selectedColor; // Current effective color
    private int manualColor; // Saved manual color
    private FloatingBottomSheet colorPickerSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur_control);

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        manualColor = prefs.getInt(PREF_BLUR_TINT_COLOR, Color.TRANSPARENT);

        initViews();
        loadSettings();
        setupListeners();

        // Candy colors removed
        // tn.eluea.kgpt.util.CandyColorHelper.applyToViewHierarchy(this,
        // findViewById(android.R.id.content));
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        switchBlurEnabled = findViewById(R.id.switch_blur_enabled);
        switchMaterialYouBlur = findViewById(R.id.switch_material_you_blur);
        customizationContainer = findViewById(R.id.customization_container);
        manualSettingsContainer = findViewById(R.id.manual_settings_container);
        sliderIntensity = findViewById(R.id.slider_intensity);
        tvIntensityValue = findViewById(R.id.tv_intensity_value);
        colorPreview = findViewById(R.id.color_preview);

        btnResetSettings = findViewById(R.id.btn_reset_settings);
        cardColorPicker = findViewById(R.id.card_color_picker);
        tvCardColorPickerTitle = findViewById(R.id.tv_blur_tint_color_title);
    }

    private void loadSettings() {
        boolean blurEnabled = prefs.getBoolean(PREF_BLUR_ENABLED, true);
        boolean materialYouBlur = prefs.getBoolean(PREF_MATERIAL_YOU_BLUR, false);
        int intensity = prefs.getInt(PREF_BLUR_INTENSITY, 25);

        switchBlurEnabled.setChecked(blurEnabled);
        switchMaterialYouBlur.setChecked(materialYouBlur);
        sliderIntensity.setValue(intensity);
        tvIntensityValue.setText(intensity + "%");

        refreshEffectiveColor();

        // Update visibility
        updateCustomizationVisibility(blurEnabled);
        updateManualSettingsVisibility(materialYouBlur);
    }

    private void refreshEffectiveColor() {
        boolean materialYouBlur = switchMaterialYouBlur.isChecked();
        if (materialYouBlur) {
            // Mix Logic: System Hue + Manual S/V
            // 1. Get System Primary Color
            int primaryColor = com.google.android.material.color.MaterialColors.getColor(
                    customizationContainer, androidx.appcompat.R.attr.colorPrimary);

            float[] primaryHsv = new float[3];
            Color.colorToHSV(primaryColor, primaryHsv);

            // 2. Get Manual Color (Last saved S/V)
            float[] manualHsv = new float[3];
            if (manualColor != Color.TRANSPARENT) {
                Color.colorToHSV(manualColor, manualHsv);
            } else {
                // Default if no manual setting: Use Primary S/V (basically full primary)
                manualHsv[1] = primaryHsv[1];
                manualHsv[2] = primaryHsv[2];
            }

            // 3. Combine: Hue from Primary, S/V from Manual
            float[] combinedHsv = new float[] { primaryHsv[0], manualHsv[1], manualHsv[2] };
            selectedColor = Color.HSVToColor(combinedHsv);

        } else {
            selectedColor = manualColor;
        }
        updateColorPreview(colorPreview, selectedColor);
        prefs.edit().putInt(PREF_BLUR_TINT_COLOR, selectedColor).apply();
    }

    private void setupListeners() {
        switchBlurEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_BLUR_ENABLED, isChecked).apply();
            updateCustomizationVisibility(isChecked);
        });

        switchMaterialYouBlur.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_MATERIAL_YOU_BLUR, isChecked).apply();
            updateManualSettingsVisibility(isChecked);
            refreshEffectiveColor();
        });

        sliderIntensity.addOnChangeListener((slider, value, fromUser) -> {
            int intValue = (int) value;
            tvIntensityValue.setText(intValue + "%");
            prefs.edit().putInt(PREF_BLUR_INTENSITY, intValue).apply();
        });

        // Color picker click - pass Material You state
        findViewById(R.id.card_color_picker)
                .setOnClickListener(v -> showColorPickerBottomSheet(switchMaterialYouBlur.isChecked()));

        // Reset Settings
        findViewById(R.id.btn_reset_settings).setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(PREF_BLUR_ENABLED, true)
                    .putBoolean(PREF_MATERIAL_YOU_BLUR, false)
                    .putInt(PREF_BLUR_INTENSITY, 25)
                    .putInt(PREF_BLUR_TINT_COLOR, Color.TRANSPARENT)
                    .apply();

            manualColor = Color.TRANSPARENT;
            selectedColor = Color.TRANSPARENT;
            loadSettings();
            Toast.makeText(this, "Settings restored", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCustomizationVisibility(boolean visible) {
        customizationContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateManualSettingsVisibility(boolean materialYouBlur) {
        // Toggle components based on Material You Blur state
        // Tint Color: Always visible in Manual Container (but Container always VISIBLE?
        // No)
        // Manual Settings Container handles Tint and Reset.
        // We want Tint VISIBLE always (but text changes). Reset GONE if MY is ON.

        manualSettingsContainer.setVisibility(View.VISIBLE); // Always visible because it contains Tint

        if (materialYouBlur) {
            btnResetSettings.setVisibility(View.GONE);
            tvCardColorPickerTitle.setText(R.string.color_saturation);
        } else {
            btnResetSettings.setVisibility(View.VISIBLE);
            tvCardColorPickerTitle.setText(R.string.blur_tint_color);
        }
    }

    private void showColorPickerBottomSheet(boolean restrictHue) {
        colorPickerSheet = new FloatingBottomSheet(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_color_picker, null);
        colorPickerSheet.setContentView(sheetView);

        // Initialize views
        tn.eluea.kgpt.ui.settings.colorpicker.SaturationValueView svView = sheetView.findViewById(R.id.sv_view);
        tn.eluea.kgpt.ui.settings.colorpicker.HueSliderView hueSlider = sheetView.findViewById(R.id.hue_slider);

        EditText etHexCode = sheetView.findViewById(R.id.et_hex_code);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save);
        View hexContainer = sheetView.findViewById(R.id.ll_hex_container);

        if (restrictHue) {
            hueSlider.setVisibility(View.GONE);
        }

        // Ensure clicking container focuses input (overriding sheetView listener)
        hexContainer.setOnClickListener(v -> {
            etHexCode.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etHexCode, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Clear focus on outside touch
        sheetView.setOnClickListener(v -> {
            etHexCode.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etHexCode.getWindowToken(), 0);
            }
        });

        final boolean[] isUpdatingFromCode = { false };

        // Initial setup
        // Use 'selectedColor' which is already the effective color (Mix or Manual)
        float[] hsv = new float[3];
        Color.colorToHSV(selectedColor, hsv);
        final float[] currentHueRef = { hsv[0] };

        svView.setHue(hsv[0]);
        svView.setSaturationValue(hsv[1], hsv[2]);
        hueSlider.setHue(hsv[0]);
        updateSaveButtonPreview(btnSave, selectedColor);
        updateSheetTint(selectedColor);

        if (selectedColor != Color.TRANSPARENT) {
            isUpdatingFromCode[0] = true;
            String hex = String.format("%08X", selectedColor);
            etHexCode.setText(hex);
            isUpdatingFromCode[0] = false;
        }

        // Listeners
        svView.setOnColorChangeListener((sat, val) -> {
            float[] currentHsv = new float[3];
            currentHsv[0] = currentHueRef[0];
            currentHsv[1] = sat;
            currentHsv[2] = val;

            int newColor = Color.HSVToColor(255, currentHsv);
            selectedColor = newColor;

            if (!isUpdatingFromCode[0]) {
                isUpdatingFromCode[0] = true;
                etHexCode.setText(String.format("%08X", selectedColor));
                isUpdatingFromCode[0] = false;
            }
            updateColorPreview(colorPreview, selectedColor);
            updateSaveButtonPreview(btnSave, selectedColor);
            updateSheetTint(selectedColor);
        });

        hueSlider.setOnHueChangeListener(hue -> {
            // If restrictHue is true, user shouldn't be able to change this,
            // but if they somehow do (e.g. keyboard nav?), we might want to block it?
            // Hiding view is usually sufficient.

            currentHueRef[0] = hue;

            float[] currentHsv = new float[3];
            currentHsv[0] = hue;

            // Get current Sat/Val from selected color
            float[] tempHsv = new float[3];
            Color.colorToHSV(selectedColor, tempHsv);
            currentHsv[1] = tempHsv[1];
            currentHsv[2] = tempHsv[2];

            svView.setHue(hue); // Update SV box hue

            int newColor = Color.HSVToColor(255, currentHsv);
            selectedColor = newColor;

            if (!isUpdatingFromCode[0]) {
                isUpdatingFromCode[0] = true;
                etHexCode.setText(String.format("%08X", selectedColor));
                isUpdatingFromCode[0] = false;
            }
            updateSaveButtonPreview(btnSave, selectedColor);
            updateSheetTint(selectedColor);
        });

        // Hex text watcher for auto-apply
        etHexCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isUpdatingFromCode[0])
                    return;

                String hex = s.toString().trim();
                if (hex.length() >= 6) {
                    try {
                        String cleanHex = hex.startsWith("#") ? hex : "#" + hex;
                        int color = Color.parseColor(cleanHex);
                        selectedColor = color;

                        float[] newHsv = new float[3];
                        Color.colorToHSV(color, newHsv);

                        // If restricted, we might want to prevent changing Hue via Hex?
                        // User prompt didn't strictly say prevent HEX hue change,
                        // just "without color slider... modify degree".
                        // Allowing HEX gives power users control.
                        // I'll allow full control via Hex.
                        currentHueRef[0] = newHsv[0];

                        isUpdatingFromCode[0] = true;
                        svView.setHue(newHsv[0]);
                        svView.setSaturationValue(newHsv[1], newHsv[2]);
                        hueSlider.setHue(newHsv[0]);
                        isUpdatingFromCode[0] = false;

                        updateSaveButtonPreview(btnSave, selectedColor);
                        updateColorPreview(colorPreview, selectedColor);
                        updateSheetTint(selectedColor);
                    } catch (Exception e) {
                        // Invalid hex, ignore
                    }
                }
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            manualColor = selectedColor;
            prefs.edit()
                    .putInt("manual_blur_tint", manualColor) // Save selection
                    .putInt(PREF_BLUR_TINT_COLOR, manualColor) // Save selection
                    .apply();

            // If MY is active, we need to refresh effective color to ensure the logic
            // persists
            // (Though refreshEffectiveColor will just use this manualColor's SV + SysHue,
            // which results in exactly 'selectedColor' if Hue didn't change).
            // Calling refreshEffectiveColor() is good practice.
            refreshEffectiveColor();

            updateColorPreview(colorPreview, selectedColor); // Handled in refresh
            colorPickerSheet.dismiss();
        });

        colorPickerSheet.show();
    }

    private void updateColorPreview(View preview, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color == Color.TRANSPARENT ? Color.WHITE : color);
        drawable.setStroke(3, ContextCompat.getColor(this, R.color.divider_color));
        preview.setBackground(drawable);
    }

    // Helper to update save button as color preview with contrast support
    private void updateSaveButtonPreview(MaterialButton btn, int color) {
        if (Color.alpha(color) == 0) {
            // Default styling if transparent/no color
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY));
            btn.setTextColor(Color.BLACK);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(Color.BLACK));
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            // Calculate luminance: 0.2126*R + 0.7152*G + 0.0722*B
            double luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color))
                    / 255.0;

            int textColor = (luminance > 0.5) ? Color.BLACK : Color.WHITE;
            btn.setTextColor(textColor);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(textColor));
        }
    }

    private void updateSheetTint(int color) {
        if (colorPickerSheet != null && colorPickerSheet.getWindow() != null) {
            if (color != Color.TRANSPARENT) {
                int alpha = 120;
                int colorWithAlpha = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
                colorPickerSheet.getWindow()
                        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(colorWithAlpha));
                colorPickerSheet.getWindow().setDimAmount(0f);
            } else {
                colorPickerSheet.getWindow()
                        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                colorPickerSheet.getWindow().setDimAmount(0.5f);
            }
        }
    }
}
