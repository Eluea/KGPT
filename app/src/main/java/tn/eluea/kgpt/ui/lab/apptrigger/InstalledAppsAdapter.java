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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tn.eluea.kgpt.R;

/**
 * Adapter for displaying installed apps in bottom sheet
 */
public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {
    
    public enum FilterType {
        ALL,
        USER,
        SYSTEM
    }
    
    private final List<InstalledApp> allApps;
    private final List<InstalledApp> filteredApps;
    private final OnAppClickListener listener;
    private final Set<String> addedPackages;
    private FilterType currentFilter = FilterType.ALL;
    private String searchQuery = "";

    public interface OnAppClickListener {
        void onAppClick(InstalledApp app);
    }

    public InstalledAppsAdapter(List<InstalledApp> apps, AppTriggerManager manager, OnAppClickListener listener) {
        this.allApps = new ArrayList<>(apps);
        this.filteredApps = new ArrayList<>(apps);
        this.listener = listener;
        
        // Cache added packages once at construction time
        this.addedPackages = new HashSet<>();
        for (AppTrigger trigger : manager.getAppTriggers()) {
            addedPackages.add(trigger.getPackageName());
        }
        
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int position) {
        return filteredApps.get(position).getPackageName().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_installed_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InstalledApp app = filteredApps.get(position);
        
        holder.tvAppName.setText(app.getAppName());
        holder.tvPackageName.setText(app.getPackageName());
        
        // Set icon directly (already loaded)
        holder.ivAppIcon.setImageDrawable(app.getIcon());
        
        // Show if already added (using cached set for performance)
        boolean isAdded = addedPackages.contains(app.getPackageName());
        holder.ivAdded.setVisibility(isAdded ? View.VISIBLE : View.GONE);
        holder.itemView.setAlpha(isAdded ? 0.5f : 1.0f);
        
        holder.itemView.setOnClickListener(v -> {
            if (!isAdded && listener != null) {
                listener.onAppClick(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
    }

    public void setFilter(FilterType filter) {
        this.currentFilter = filter;
        applyFilters();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase().trim();
        applyFilters();
    }

    private void applyFilters() {
        filteredApps.clear();
        
        for (InstalledApp app : allApps) {
            // Apply filter type
            boolean passesFilter = true;
            switch (currentFilter) {
                case USER:
                    passesFilter = !app.isSystemApp();
                    break;
                case SYSTEM:
                    passesFilter = app.isSystemApp();
                    break;
                case ALL:
                default:
                    passesFilter = true;
                    break;
            }
            
            // Apply search query
            boolean passesSearch = searchQuery.isEmpty() ||
                    app.getAppName().toLowerCase().contains(searchQuery) ||
                    app.getPackageName().toLowerCase().contains(searchQuery);
            
            if (passesFilter && passesSearch) {
                filteredApps.add(app);
            }
        }
        
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvPackageName;
        ImageView ivAdded;

        ViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            ivAdded = itemView.findViewById(R.id.iv_added);
        }
    }
}
