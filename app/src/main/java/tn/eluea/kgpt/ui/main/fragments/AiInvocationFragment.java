/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.MainActivity;

public class AiInvocationFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FrameLayout btnInfo;

    // Fragments
    private InvocationCommandsFragment commandsFragment;
    private InvocationPatternsFragment patternsFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_invocation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        btnInfo = view.findViewById(R.id.btn_info);

        setupViewPager();
        // Setup initial Dock State
        updateFabState(0);

        btnInfo.setOnClickListener(v -> showInfoBottomSheet());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore standard navigation dock
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showDockNavigation();
        }
    }

    private void setupViewPager() {
        commandsFragment = new InvocationCommandsFragment();
        patternsFragment = new InvocationPatternsFragment();

        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0)
                    return commandsFragment;
                return patternsFragment;
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View customView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tab_lottie, null);
            com.airbnb.lottie.LottieAnimationView lottieView = customView.findViewById(R.id.tab_lottie_icon);
            android.widget.TextView tvTitle = customView.findViewById(R.id.tab_title);

            if (position == 0) {
                tvTitle.setText(R.string.tab_commands);
                lottieView.setAnimation("lottie/system-solid-369-code-hover-code.json");
            } else {
                tvTitle.setText(R.string.tab_triggers);
                lottieView.setAnimation("lottie/system-solid-4079-hand-swipe-right-left-hover-swipe.json");
            }

            tab.setCustomView(customView);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateCustomTabState(tab, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateCustomTabState(tab, false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateCustomTabState(tab, true);
            }
        });

        // Initialize state for tabs
        tabLayout.post(() -> {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null) {
                    updateCustomTabState(tab, i == viewPager.getCurrentItem());
                }
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateFabState(position);
            }
        });
    }

    private void updateCustomTabState(TabLayout.Tab tab, boolean isSelected) {
        if (!isAdded() || getContext() == null) return;
        View customView = tab.getCustomView();
        if (customView == null) return;

        com.airbnb.lottie.LottieAnimationView lottieView = customView.findViewById(R.id.tab_lottie_icon);
        android.widget.TextView tvTitle = customView.findViewById(R.id.tab_title);

        int activeColor = com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE);
        int inactiveColor = com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.GRAY);

        if (isSelected) {
            if (tvTitle != null) tvTitle.setTextColor(activeColor);
            tn.eluea.kgpt.util.LottieHelper.playOnce(lottieView, activeColor);
        } else {
            if (tvTitle != null) tvTitle.setTextColor(inactiveColor);
            tn.eluea.kgpt.util.LottieHelper.setStaticFrame(lottieView, 0, inactiveColor);
        }
    }

    private void updateFabState(int position) {
        if (!(requireActivity() instanceof MainActivity))
            return;
        MainActivity activity = (MainActivity) requireActivity();

        if (position == 0) {
            activity.setDockAction(getString(R.string.dock_add_command), R.drawable.ic_add, v -> {
                if (commandsFragment != null && commandsFragment.isAdded()) {
                    commandsFragment.showAddCommandDialog();
                }
            });
        } else {
            activity.setDockAction(getString(R.string.dock_how_to_use), R.drawable.ic_info_circle_filled, v -> {
                if (patternsFragment != null && patternsFragment.isAdded()) {
                    patternsFragment.showHowToUse();
                }
            });
        }
    }

    private void showInfoBottomSheet() {
        if (patternsFragment != null) {
            patternsFragment.showHowToUse();
        } else {
            // Fallback if needed
        }
    }
}
