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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;

public class ApiKeysAdapter extends RecyclerView.Adapter<ApiKeysAdapter.ApiKeyViewHolder> {

    private final Context context;
    private final List<LanguageModel> models;

    public ApiKeysAdapter(Context context, List<LanguageModel> models) {
        this.context = context;
        this.models = models;
    }

    @NonNull
    @Override
    public ApiKeyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_api_key, parent, false);
        return new ApiKeyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApiKeyViewHolder holder, int position) {
        LanguageModel model = models.get(position);
        holder.bind(model);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    class ApiKeyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvProviderName;
        private final TextInputEditText etApiKey;
        private final MaterialButton btnSaveKey;
        private final ImageView ivStatus;

        ApiKeyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProviderName = itemView.findViewById(R.id.tv_provider_name);
            etApiKey = itemView.findViewById(R.id.et_api_key);
            btnSaveKey = itemView.findViewById(R.id.btn_save_key);
            ivStatus = itemView.findViewById(R.id.iv_status);
        }

        void bind(LanguageModel model) {
            tvProviderName.setText(model.label);

            // Load existing API key
            if (SPManager.isReady()) {
                String apiKey = SPManager.getInstance().getApiKey(model);
                if (apiKey != null && !apiKey.isEmpty()) {
                    etApiKey.setText(apiKey);
                    ivStatus.setVisibility(View.VISIBLE);
                } else {
                    etApiKey.setText("");
                    ivStatus.setVisibility(View.GONE);
                }
            }

            btnSaveKey.setOnClickListener(v -> {
                String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
                
                if (SPManager.isReady()) {
                    SPManager.getInstance().setApiKey(model, apiKey);
                    
                    if (!apiKey.isEmpty()) {
                        ivStatus.setVisibility(View.VISIBLE);
                        Toast.makeText(context, model.label + " API key saved", Toast.LENGTH_SHORT).show();
                    } else {
                        ivStatus.setVisibility(View.GONE);
                        Toast.makeText(context, model.label + " API key removed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
