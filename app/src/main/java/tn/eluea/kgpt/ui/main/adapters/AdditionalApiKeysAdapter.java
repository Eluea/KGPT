/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;

public class AdditionalApiKeysAdapter extends RecyclerView.Adapter<AdditionalApiKeysAdapter.ViewHolder> {

    private final Context context;
    private final List<LanguageModel> models;
    private final OnKeyActionListener listener;

    public interface OnKeyActionListener {
        void onKeySaved(LanguageModel model, String key);

        void onKeyDeleted(LanguageModel model, int position);

        void onGetKeyClicked(LanguageModel model);
    }

    public AdditionalApiKeysAdapter(Context context, List<LanguageModel> models, OnKeyActionListener listener) {
        this.context = context;
        this.models = models;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_additional_api_key, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguageModel model = models.get(position);
        holder.bind(model, position);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvProviderName;
        private final ImageView ivProviderIcon;
        private final TextInputEditText etApiKey;
        private final ImageView ivStatus;
        private final ImageView btnDelete;
        private final MaterialButton btnSaveKey;
        private final MaterialButton btnGetKey;
        private final com.google.android.material.card.MaterialCardView cvApiStatus;
        private final TextView tvApiStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProviderName = itemView.findViewById(R.id.tv_provider_name);
            ivProviderIcon = itemView.findViewById(R.id.iv_provider_icon);
            etApiKey = itemView.findViewById(R.id.et_api_key);
            ivStatus = itemView.findViewById(R.id.iv_status);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnSaveKey = itemView.findViewById(R.id.btn_save_key);
            btnGetKey = itemView.findViewById(R.id.btn_get_key);
            cvApiStatus = itemView.findViewById(R.id.cv_api_status);
            tvApiStatus = itemView.findViewById(R.id.tv_api_status);
        }

        void bind(LanguageModel model, int position) {
            tvProviderName.setText(model.label);
            if (ivProviderIcon != null) {
                ivProviderIcon.setImageResource(tn.eluea.kgpt.util.ProviderLogoHelper.getLogoRes(model));
            }

            if (model.isFree) {
                tvApiStatus.setText(R.string.label_free_api);
                cvApiStatus.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(resolveAttrColor(
                        itemView.getContext(), com.google.android.material.R.attr.colorPrimaryContainer)));
                tvApiStatus.setTextColor(resolveAttrColor(itemView.getContext(),
                        com.google.android.material.R.attr.colorOnPrimaryContainer));
            } else {
                tvApiStatus.setText(R.string.label_paid_api);
                cvApiStatus.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(resolveAttrColor(
                        itemView.getContext(), com.google.android.material.R.attr.colorTertiaryContainer)));
                tvApiStatus.setTextColor(resolveAttrColor(itemView.getContext(),
                        com.google.android.material.R.attr.colorOnTertiaryContainer));
            }

            // Load existing key
            if (SPManager.isReady()) {
                String key = SPManager.getInstance().getApiKey(model);
                if (key != null && !key.isEmpty()) {
                    etApiKey.setText(key);
                    ivStatus.setVisibility(View.VISIBLE);
                } else {
                    etApiKey.setText("");
                    ivStatus.setVisibility(View.GONE);
                }
            }

            // Scroll to this item when focused
            etApiKey.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Post delayed to allow keyboard to appear first
                    v.postDelayed(() -> {
                        // Request rectangle on screen to force scroll
                        android.graphics.Rect rect = new android.graphics.Rect(0, 0, v.getWidth(), v.getHeight());
                        v.requestRectangleOnScreen(rect, true);
                    }, 300);
                }
            });

            btnSaveKey.setOnClickListener(v -> {
                String key = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
                if (!key.isEmpty() && listener != null) {
                    listener.onKeySaved(model, key);
                    ivStatus.setVisibility(View.VISIBLE);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onKeyDeleted(model, position);
                }
            });

            btnGetKey.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGetKeyClicked(model);
                }
            });
        }

        private int resolveAttrColor(Context context, int attr) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(attr, typedValue, true);
            return typedValue.data;
        }
    }
}
