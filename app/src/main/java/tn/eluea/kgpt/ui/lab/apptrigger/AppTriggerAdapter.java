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
package tn.eluea.kgpt.ui.lab.apptrigger;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import tn.eluea.kgpt.R;

/**
 * Adapter for displaying app triggers in RecyclerView
 */
public class AppTriggerAdapter extends RecyclerView.Adapter<AppTriggerAdapter.ViewHolder> {
    
    private final List<AppTrigger> triggers;
    private final Context context;
    private final OnTriggerClickListener listener;

    public interface OnTriggerClickListener {
        void onTriggerClick(AppTrigger trigger);
        void onTriggerToggle(AppTrigger trigger, boolean enabled);
    }

    public AppTriggerAdapter(Context context, List<AppTrigger> triggers, OnTriggerClickListener listener) {
        this.context = context;
        this.triggers = triggers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_trigger, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppTrigger trigger = triggers.get(position);
        
        holder.tvAppName.setText(trigger.getAppName());
        holder.tvPackageName.setText(trigger.getPackageName());
        holder.tvTrigger.setText("Trigger: " + trigger.getTrigger());
        
        // Load app icon
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(trigger.getPackageName());
            holder.ivAppIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.ivAppIcon.setImageResource(R.drawable.ic_cpu_filled);
        }
        
        holder.switchEnabled.setChecked(trigger.isEnabled());
        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTriggerToggle(trigger, isChecked);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTriggerClick(trigger);
            }
        });
    }

    @Override
    public int getItemCount() {
        return triggers.size();
    }

    public void updateData(List<AppTrigger> newTriggers) {
        triggers.clear();
        triggers.addAll(newTriggers);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvPackageName;
        TextView tvTrigger;
        MaterialSwitch switchEnabled;

        ViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            tvTrigger = itemView.findViewById(R.id.tv_trigger);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
        }
    }
}
