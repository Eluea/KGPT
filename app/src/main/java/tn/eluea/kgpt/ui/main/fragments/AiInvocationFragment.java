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
            if (position == 0) {
                tab.setText("Commands");
                tab.setIcon(R.drawable.ic_command_filled);
            } else {
                tab.setText("Triggers");
                tab.setIcon(R.drawable.ic_keyboard_filled);
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateFabState(position);
            }
        });
    }

    private void updateFabState(int position) {
        if (!(requireActivity() instanceof MainActivity))
            return;
        MainActivity activity = (MainActivity) requireActivity();

        if (position == 0) {
            activity.setDockAction("Add Command", R.drawable.ic_add, v -> {
                if (commandsFragment != null && commandsFragment.isAdded()) {
                    commandsFragment.showAddCommandDialog();
                }
            });
        } else {
            activity.setDockAction("How to Use", R.drawable.ic_info_circle_filled, v -> {
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
