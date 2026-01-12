package tn.eluea.kgpt.ui.lab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import tn.eluea.kgpt.R;

import tn.eluea.kgpt.ui.main.MainActivity;

public class LabFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        applyAmoledIfNeeded();
        // Lab is now a main navigation tab, so we don't set dock action here
        // The navigation dock will be shown by default
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Lab is a main navigation tab, nothing to clean up
    }

    private void initViews(View view) {
        // Hide back button since Lab is now a main navigation tab
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }

        // App Triggers feature
        view.findViewById(R.id.card_app_triggers).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToAppTrigger();
            }
        });

        // Text Actions feature
        view.findViewById(R.id.card_text_actions).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTextActions();
            }
        });

    }

    private void applyAmoledIfNeeded() {
        boolean isDarkMode = tn.eluea.kgpt.ui.main.BottomSheetHelper.isDarkMode(requireContext());
        boolean isAmoled = tn.eluea.kgpt.ui.main.BottomSheetHelper.isAmoledMode(requireContext());

        if (isDarkMode && isAmoled) {
            View root = getView() != null ? getView().findViewById(R.id.root_layout) : null;
            if (root != null) {
                root.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.background_amoled));
            }
        }
    }
}
