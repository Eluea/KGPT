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
package tn.eluea.kgpt.ui.lab;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerActivity;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;

public class LabActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme is now applied globally by KGPTApplication
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);
        
        initViews();
        applyAmoledIfNeeded();
    }
    
    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        // App Triggers feature
        findViewById(R.id.card_app_triggers).setOnClickListener(v -> {
            startActivity(new Intent(this, AppTriggerActivity.class));
        });
    }
    
    /**
     * Apply AMOLED-specific colors if enabled.
     * Note: Dark mode is handled globally by KGPTApplication.
     */
    private void applyAmoledIfNeeded() {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(this);
        boolean isAmoled = BottomSheetHelper.isAmoledMode(this);
        
        if (isDarkMode && isAmoled) {
            View root = findViewById(R.id.root_layout);
            root.setBackgroundColor(ContextCompat.getColor(this, R.color.background_amoled));
        }
        
        // Update status bar icons for dark mode
        if (isDarkMode) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }
}
