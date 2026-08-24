/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.settings.colorpicker.HueSliderView;
import tn.eluea.kgpt.ui.settings.colorpicker.SaturationValueView;
import tn.eluea.kgpt.util.MaterialYouManager;

public class MaterialYouSettingsActivity extends AppCompatActivity {

    private MaterialYouManager manager;
    private MaterialSwitch switchWallpaper;

    private View seedColorPreview;
    private MaterialCardView cardWallpaper;
    private MaterialCardView cardSeedColor;
    private LinearLayout containerCustomization;

    // Color Picker State
    private int selectedColor; // Temporary selection in picker

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_you_settings);
        manager = MaterialYouManager.getInstance(this);

        initViews();
        setupListeners();
        updateUI();

        // Candy colors removed
        // tn.eluea.kgpt.util.CandyColorHelper.applyToViewHierarchy(this,
        // findViewById(android.R.id.content));
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        switchWallpaper = findViewById(R.id.switch_wallpaper_source);

        seedColorPreview = findViewById(R.id.seed_color_preview);
        cardWallpaper = findViewById(R.id.card_wallpaper_source);
        cardSeedColor = findViewById(R.id.card_seed_color);
        containerCustomization = findViewById(R.id.customization_container);
    }

    private void setupListeners() {

        switchWallpaper.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                manager.setUseWallpaperSource(isChecked);
                tn.eluea.kgpt.KGPTApplication.getInstance().recreateAllActivities();
            }
        });

        cardSeedColor.setOnClickListener(v -> showColorPickerBottomSheet());
    }

    private void updateUI() {
        boolean isEnabled = true; // Always enabled
        // switchEnabled.setChecked(isEnabled); // Removed
        containerCustomization.setVisibility(View.VISIBLE);

        if (isEnabled) {
            boolean isWallpaper = manager.isUseWallpaperSource();
            boolean isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

            if (isSupported) {
                cardWallpaper.setVisibility(View.VISIBLE);
                switchWallpaper.setChecked(isWallpaper);
            } else {
                cardWallpaper.setVisibility(View.GONE);
                // On unsupported devices, we ignore wallpaper setting visually
            }

            // Show seed color if wallpaper is OFF or device is unsupported
            boolean showSeed = !isSupported || !isWallpaper;
            if (showSeed) {
                cardSeedColor.setVisibility(View.VISIBLE);
                updateColorPreview(seedColorPreview, manager.getSeedColor());
            } else {
                cardSeedColor.setVisibility(View.GONE);
            }

        }
    }

    private void updateColorPreview(View preview, int color) {
        if (preview == null)
            return;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color == Color.TRANSPARENT ? Color.WHITE : color);
        drawable.setStroke(3, ContextCompat.getColor(this, R.color.divider_color));
        preview.setBackground(drawable);
    }

    // --- Color Picker Logic (Adapted from BlurControlActivity) ---

    private void showColorPickerBottomSheet() {
        selectedColor = manager.getSeedColor();

        MaterialYouColorSheet colorPickerSheet = new MaterialYouColorSheet(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_material_you_color, null);
        colorPickerSheet.setContentView(sheetView);

        // Initialize views
        SaturationValueView svView = sheetView.findViewById(R.id.sv_view);
        HueSliderView hueSlider = sheetView.findViewById(R.id.hue_slider);
        EditText etHexCode = sheetView.findViewById(R.id.et_hex_code);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save);
        View hexContainer = sheetView.findViewById(R.id.ll_hex_container);
        View colorPreview = sheetView.findViewById(R.id.color_preview); // Small preview in sheet

        // Ensure clicking container focuses input
        hexContainer.setOnClickListener(v -> {
            etHexCode.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etHexCode, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Clear focus on outside touch
        sheetView.setOnClickListener(v -> {
            etHexCode.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etHexCode.getWindowToken(), 0);
            }
        });

        final boolean[] isUpdatingFromCode = { false };

        // Use a final reference to the sheet for the inner classes/lambdas
        final MaterialYouColorSheet finalSheet = colorPickerSheet;

        // Visual helper for tint - applied to PREVIEW the selection visually
        // but decoupled from blur settings.
        // Removed full window tinting to avoid confusion with Blur Control.
        // The preview is handled by updateColorPreview()

        // Initial setup
        float[] hsv = new float[3];
        Color.colorToHSV(selectedColor, hsv);
        final float[] currentHueRef = { hsv[0] };

        svView.setHue(hsv[0]);
        svView.setSaturationValue(hsv[1], hsv[2]);
        hueSlider.setHue(hsv[0]);
        updateSaveButtonPreview(btnSave, selectedColor);
        updateColorPreview(colorPreview, selectedColor);

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

        });

        hueSlider.setOnHueChangeListener(hue -> {
            currentHueRef[0] = hue;
            float[] currentHsv = new float[3];
            currentHsv[0] = hue;

            float[] tempHsv = new float[3];
            Color.colorToHSV(selectedColor, tempHsv);
            currentHsv[1] = tempHsv[1];
            currentHsv[2] = tempHsv[2];

            svView.setHue(hue);

            int newColor = Color.HSVToColor(255, currentHsv);
            selectedColor = newColor;

            if (!isUpdatingFromCode[0]) {
                isUpdatingFromCode[0] = true;
                etHexCode.setText(String.format("%08X", selectedColor));
                isUpdatingFromCode[0] = false;
            }
            updateSaveButtonPreview(btnSave, selectedColor);

        });

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
                        currentHueRef[0] = newHsv[0];
                        isUpdatingFromCode[0] = true;
                        svView.setHue(newHsv[0]);
                        svView.setSaturationValue(newHsv[1], newHsv[2]);
                        hueSlider.setHue(newHsv[0]);
                        isUpdatingFromCode[0] = false;
                        updateSaveButtonPreview(btnSave, selectedColor);
                        updateColorPreview(colorPreview, selectedColor);

                    } catch (Exception e) {
                    }
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            manager.setSeedColor(selectedColor);
            colorPickerSheet.dismiss();
            tn.eluea.kgpt.KGPTApplication.getInstance().recreateAllActivities();
            Toast.makeText(this, R.string.msg_theme_updated, Toast.LENGTH_SHORT).show();
        });

        colorPickerSheet.show();
    }

    private void updateSaveButtonPreview(MaterialButton btn, int color) {
        if (Color.alpha(color) == 0) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
            btn.setTextColor(Color.BLACK);
            btn.setIconTint(ColorStateList.valueOf(Color.BLACK));
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(color));
            double luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color))
                    / 255.0;
            int textColor = (luminance > 0.5) ? Color.BLACK : Color.WHITE;
            btn.setTextColor(textColor);
            btn.setIconTint(ColorStateList.valueOf(textColor));
        }
    }

    // --- Restart Logic ---

    // Changing strategy: The user wants it to WORK.
    // Immediate feedback is best.
    // Let's call recreate() when settings change.

}
