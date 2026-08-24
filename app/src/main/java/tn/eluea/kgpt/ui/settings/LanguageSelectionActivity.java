/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.MainActivity;
import tn.eluea.kgpt.util.LocaleHelper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class LanguageSelectionActivity extends AppCompatActivity implements LanguageAdapter.OnLanguageSelectedListener {

    private LanguageAdapter adapter;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme using MaterialYouManager as used in other activities

        setContentView(R.layout.activity_language_selection);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        etSearch = findViewById(R.id.et_search);
        RecyclerView rvLanguages = findViewById(R.id.rv_languages);
        rvLanguages.setLayoutManager(new LinearLayoutManager(this));

        List<LanguageItem> languages = getLanguages();
        adapter = new LanguageAdapter(this, languages, this);
        rvLanguages.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private List<LanguageItem> getLanguages() {
        // String array from resources: "English", "العربية (Arabic)", etc.
        // Or "ar", "en", etc from code?
        // Let's replicate what was in SettingsFragment
        String[] languagesArray = getResources().getStringArray(R.array.languages_list);
        String[] languageCodes = new String[] {
                "ar", "en", "fr", "es", "de", "it", "ru", "tr", "zh", "ja", "ko", "hi", "pt", "in", "bn", "vi", "th"
        };

        String currentLangCode = LocaleHelper.getLanguage(this);
        List<LanguageItem> list = new ArrayList<>();

        for (int i = 0; i < languageCodes.length; i++) {
            String rawName = (i < languagesArray.length) ? languagesArray[i] : languageCodes[i];
            // Format: "Native (English)" or just "Native"
            // Let's parse or just use rawName as Name and Code as Native for simplicity
            // unless we want more detail

            // Actually, let's create cleaner names
            String code = languageCodes[i];
            Locale locale = new Locale(code);
            String displayName = locale.getDisplayName(locale); // Native name: العربية, English, Español
            String englishName = locale.getDisplayName(Locale.ENGLISH); // Arabic, English, Spanish

            String nameToShow = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            String subName = nameToShow.equals(englishName) ? "" : englishName;

            // Use the rawName from array if needed, but dynamic Locale is cleaner
            // The array has format: "العربية (Arabic)"
            String combinedName = (i < languagesArray.length) ? languagesArray[i] : code;

            // Primary name from resources (Curated native name)
            String primaryName = (i < languagesArray.length) ? languagesArray[i] : code;

            // Clean up if it still has parens (legacy support)
            if (primaryName.contains("(")) {
                int start = primaryName.indexOf("(");
                if (start > 0) {
                    primaryName = primaryName.substring(0, start).trim();
                }
            }

            // Secondary name from Locale (English name)
            String secondaryName = locale.getDisplayName(Locale.ENGLISH);
            // Capitalize first letter
            if (secondaryName.length() > 0) {
                secondaryName = secondaryName.substring(0, 1).toUpperCase() + secondaryName.substring(1);
            }

            // If primary (Native) and secondary (English) are roughly the same, KEKK them
            // both visible
            // User requested: "even English should be like that" (Native + English below)
            // So we DO NOT clear secondaryName even if it equals primaryName.

            boolean isSelected = code.equals(currentLangCode);
            list.add(new LanguageItem(code, primaryName, secondaryName, isSelected));
        }
        return list;
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLanguageSelected(LanguageItem language) {
        LocaleHelper.setLocale(this, language.getCode());

        // Restart app to apply changes fully
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
