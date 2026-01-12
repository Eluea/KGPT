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
package tn.eluea.kgpt.ui.main.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;

public class PatternsAdapter extends RecyclerView.Adapter<PatternsAdapter.PatternViewHolder> {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";
    
    private List<ParsePattern> patterns;
    private OnPatternClickListener listener;
    private boolean isAmoledMode = false;

    public interface OnPatternClickListener {
        void onPatternClick(ParsePattern pattern, int position);
        void onPatternDelete(ParsePattern pattern, int position);
    }

    public PatternsAdapter(List<ParsePattern> patterns, OnPatternClickListener listener) {
        this.patterns = patterns;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Check AMOLED mode from preferences
        Context context = parent.getContext();
        SharedPreferences prefs = context.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);
        isAmoledMode = isDarkMode && prefs.getBoolean(PREF_AMOLED, false);
        
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pattern, parent, false);
        return new PatternViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatternViewHolder holder, int position) {
        ParsePattern pattern = patterns.get(position);
        holder.bind(pattern, position);
    }

    @Override
    public int getItemCount() {
        return patterns != null ? patterns.size() : 0;
    }

    public void updatePatterns(List<ParsePattern> newPatterns) {
        this.patterns = newPatterns;
        notifyDataSetChanged();
    }

    class PatternViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatternName;
        private TextView tvPatternRegex;
        private TextView tvPatternDescription;
        private TextView tvPatternExample;
        private ImageView ivArrow;

        PatternViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatternName = itemView.findViewById(R.id.tv_pattern_name);
            tvPatternRegex = itemView.findViewById(R.id.tv_pattern_regex);
            tvPatternDescription = itemView.findViewById(R.id.tv_pattern_description);
            tvPatternExample = itemView.findViewById(R.id.tv_pattern_example);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
        }

        void bind(ParsePattern pattern, int position) {
            // Show enabled/disabled status in title
            String title = pattern.getType().title;
            if (!pattern.isEnabled()) {
                title += " (Disabled)";
            }
            tvPatternName.setText(title);
            
            // Show user-friendly symbol in chip (without "Trigger:" prefix)
            String displaySymbol = getDisplaySymbol(pattern);
            tvPatternRegex.setText(displaySymbol);
            
            // Show description
            if (tvPatternDescription != null) {
                tvPatternDescription.setText(pattern.getType().description);
                tvPatternDescription.setVisibility(View.VISIBLE);
            }
            
            // Show custom example for each pattern type
            if (tvPatternExample != null) {
                String example = getExampleForPattern(pattern);
                tvPatternExample.setText("e.g. " + example);
                tvPatternExample.setVisibility(View.VISIBLE);
                // Apply AMOLED background if needed
                if (isAmoledMode) {
                    tvPatternExample.setBackgroundResource(R.drawable.bg_example_chip_amoled);
                }
            }

            // Show arrow for all editable patterns
            if (ivArrow != null) {
                ivArrow.setVisibility(pattern.getType().editable ? View.VISIBLE : View.INVISIBLE);
            }
            
            // Set alpha based on enabled state
            itemView.setAlpha(pattern.isEnabled() ? 1.0f : 0.5f);

            itemView.setOnClickListener(v -> {
                if (listener != null && pattern.getType().editable) {
                    listener.onPatternClick(pattern, position);
                }
            });
        }
        
        /**
         * Extract and display user-friendly symbol from pattern (without quotes)
         */
        private String getDisplaySymbol(ParsePattern pattern) {
            String regex = pattern.getPattern().pattern();
            String symbol = PatternType.regexToSymbol(regex);
            
            if (symbol != null && !symbol.isEmpty()) {
                return symbol;
            }
            
            // Fallback to default symbol
            return pattern.getType().defaultSymbol;
        }
        
        /**
         * Get custom example for each pattern type
         */
        private String getExampleForPattern(ParsePattern pattern) {
            String symbol = PatternType.regexToSymbol(pattern.getPattern().pattern());
            if (symbol == null) {
                symbol = pattern.getType().defaultSymbol;
            }
            
            switch (pattern.getType()) {
                case CommandAI:
                    return "Write a poem about nature" + symbol;
                case CommandCustom:
                    return "Hello world /translate" + symbol;
                case FormatItalic:
                    return "important text" + symbol;
                case FormatBold:
                    return "highlight this" + symbol;
                case FormatCrossout:
                    return "deleted text" + symbol;
                case FormatUnderline:
                    return "underlined text" + symbol;
                case WebSearch:
                    return "latest news about AI" + symbol;
                case Settings:
                    return symbol;
                default:
                    return "your text here" + symbol;
            }
        }
    }
}
