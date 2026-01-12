/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package tn.eluea.kgpt.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class AiSettingsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FrameLayout btnInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupInfoButton();
        applyAmoledIfNeeded();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        btnInfo = view.findViewById(R.id.btn_info);
    }

    private void setupTabs() {
        AiSettingsPagerAdapter adapter = new AiSettingsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Model");
                    tab.setIcon(R.drawable.ic_model_default);
                    break;
                case 1:
                    tab.setText("API Keys");
                    tab.setIcon(R.drawable.ic_key_filled);
                    break;
            }
        }).attach();
    }

    private void setupInfoButton() {
        btnInfo.setOnClickListener(v -> showInfoBottomSheet());
    }

    private void showInfoBottomSheet() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ai_usage, null);

        FloatingBottomSheet bottomSheet = new FloatingBottomSheet(requireContext());
        bottomSheet.setContentView(view);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void applyAmoledIfNeeded() {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(requireContext());
        boolean isAmoled = BottomSheetHelper.isAmoledMode(requireContext());

        if (isDarkMode && isAmoled) {
            View root = getView() != null ? getView().findViewById(R.id.root_layout) : null;
            if (root != null) {
                root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
            }
        }
    }

    // ViewPager2 Adapter
    private static class AiSettingsPagerAdapter extends FragmentStateAdapter {

        public AiSettingsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new ModelsFragment();
                case 1:
                    return new ApiKeysFragment();
                default:
                    return new ModelsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
