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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.llm.LanguageModel;

public class ModelsAdapter extends RecyclerView.Adapter<ModelsAdapter.ModelViewHolder> {

    private final List<LanguageModel> models;
    private LanguageModel selectedModel;
    private final OnModelSelectedListener listener;

    public interface OnModelSelectedListener {
        void onModelSelected(LanguageModel model);
    }

    public ModelsAdapter(List<LanguageModel> models, LanguageModel selectedModel, OnModelSelectedListener listener) {
        this.models = models;
        this.selectedModel = selectedModel;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        LanguageModel model = models.get(position);
        holder.bind(model, model == selectedModel);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    class ModelViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardModel;
        private final FrameLayout iconContainer;
        private final ImageView ivModelIcon;
        private final TextView tvModelName;
        private final ImageView ivSelected;

        ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            cardModel = itemView.findViewById(R.id.card_model);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivModelIcon = itemView.findViewById(R.id.iv_model_icon);
            tvModelName = itemView.findViewById(R.id.tv_model_name);
            ivSelected = itemView.findViewById(R.id.iv_selected);
        }

        void bind(LanguageModel model, boolean isSelected) {
            tvModelName.setText(model.label);

            if (isSelected) {
                cardModel.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                cardModel.setStrokeWidth(3);
                ivSelected.setVisibility(View.VISIBLE);
                ivModelIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            } else {
                cardModel.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.divider_light));
                cardModel.setStrokeWidth(1);
                ivSelected.setVisibility(View.GONE);
                ivModelIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.dock_item_inactive));
            }

            cardModel.setOnClickListener(v -> {
                int previousSelected = models.indexOf(selectedModel);
                selectedModel = model;
                notifyItemChanged(previousSelected);
                notifyItemChanged(getAdapterPosition());
                if (listener != null) {
                    listener.onModelSelected(model);
                }
            });
        }
    }
}
