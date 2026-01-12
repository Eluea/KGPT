package tn.eluea.kgpt.ui.lab.apptrigger;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.MainActivity;

public class AppTriggerFragment extends Fragment implements AppTriggerAdapter.OnTriggerClickListener {

    private AppTriggerManager manager;
    private AppTriggerAdapter adapter;
    private RecyclerView recyclerView;
    private MaterialSwitch switchFeatureEnabled;
    private TextView tvEmptyState;
    private MaterialCardView exampleContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_trigger, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        manager = new AppTriggerManager(requireContext());

        initViews(view);
        applyAmoledIfNeeded(view);
        loadData();

        // Update Dock Action to "Add App Trigger"
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setDockAction("Add App Trigger", R.drawable.ic_add, v -> {
                showAppSelectionBottomSheet();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Note: Don't call showDockNavigation() here. When going back to LabFragment,
        // LabFragment.onViewCreated will set its dock action. The dock lifecycle is
        // managed
        // by each fragment's onViewCreated and by BackStackChangedListener in
        // MainActivity.
    }

    private void initViews(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        switchFeatureEnabled = view.findViewById(R.id.switch_feature_enabled);
        switchFeatureEnabled.setChecked(manager.isFeatureEnabled());
        switchFeatureEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            manager.setFeatureEnabled(isChecked);
            updateEmptyState();
        });

        recyclerView = view.findViewById(R.id.recycler_triggers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        exampleContainer = view.findViewById(R.id.example_container);
    }

    private void loadData() {
        List<AppTrigger> triggers = manager.getAppTriggers();
        adapter = new AppTriggerAdapter(requireContext(), new ArrayList<>(triggers), this);
        recyclerView.setAdapter(adapter);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean hasTriggers = !manager.getAppTriggers().isEmpty();
        tvEmptyState.setVisibility(hasTriggers ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hasTriggers ? View.VISIBLE : View.GONE);
        exampleContainer.setVisibility(hasTriggers ? View.VISIBLE : View.GONE);
    }

    private void showAppSelectionBottomSheet() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_app_selection, null);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(view);

        RecyclerView rvApps = view.findViewById(R.id.rv_apps);
        EditText etSearch = view.findViewById(R.id.et_search);
        TextView tvLoading = view.findViewById(R.id.tv_loading);

        // Filter chips
        MaterialCardView chipAll = view.findViewById(R.id.chip_all);
        MaterialCardView chipUser = view.findViewById(R.id.chip_user);
        MaterialCardView chipSystem = view.findViewById(R.id.chip_system);
        TextView tvChipAll = view.findViewById(R.id.tv_chip_all);
        TextView tvChipUser = view.findViewById(R.id.tv_chip_user);
        TextView tvChipSystem = view.findViewById(R.id.tv_chip_system);

        rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load apps in background
        new Thread(() -> {
            List<InstalledApp> apps = manager.getInstalledApps();
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(() -> {
                tvLoading.setVisibility(View.GONE);
                rvApps.setVisibility(View.VISIBLE);

                InstalledAppsAdapter appsAdapter = new InstalledAppsAdapter(apps, manager, app -> {
                    dialog.dismiss();
                    showTriggerEditDialog(app, null);
                });
                rvApps.setAdapter(appsAdapter);
                rvApps.setItemViewCacheSize(20);

                // Search functionality
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        appsAdapter.setSearchQuery(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                // Filter chip click handlers
                chipAll.setOnClickListener(v -> {
                    appsAdapter.setFilter(InstalledAppsAdapter.FilterType.ALL);
                    updateFilterChips(chipAll, chipUser, chipSystem, tvChipAll, tvChipUser, tvChipSystem, 0);
                });

                chipUser.setOnClickListener(v -> {
                    appsAdapter.setFilter(InstalledAppsAdapter.FilterType.USER);
                    updateFilterChips(chipAll, chipUser, chipSystem, tvChipAll, tvChipUser, tvChipSystem, 1);
                });

                chipSystem.setOnClickListener(v -> {
                    appsAdapter.setFilter(InstalledAppsAdapter.FilterType.SYSTEM);
                    updateFilterChips(chipAll, chipUser, chipSystem, tvChipAll, tvChipUser, tvChipSystem, 2);
                });
            });
        }).start();

        dialog.show();
    }

    private void updateFilterChips(MaterialCardView chipAll, MaterialCardView chipUser,
            MaterialCardView chipSystem, TextView tvChipAll,
            TextView tvChipUser, TextView tvChipSystem, int selected) {

        int primaryColor = com.google.android.material.color.MaterialColors.getColor(chipAll,
                androidx.appcompat.R.attr.colorPrimary);
        int onPrimaryColor = com.google.android.material.color.MaterialColors.getColor(chipAll,
                com.google.android.material.R.attr.colorOnPrimary);
        int surfaceVariant = com.google.android.material.color.MaterialColors.getColor(chipAll,
                com.google.android.material.R.attr.colorSurfaceVariant);
        int onSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(chipAll,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        // Reset all chips to inactive state
        chipAll.setCardBackgroundColor(surfaceVariant);
        chipUser.setCardBackgroundColor(surfaceVariant);
        chipSystem.setCardBackgroundColor(surfaceVariant);

        tvChipAll.setTextColor(onSurfaceVariant);
        tvChipAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvChipUser.setTextColor(onSurfaceVariant);
        tvChipUser.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvChipSystem.setTextColor(onSurfaceVariant);
        tvChipSystem.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Highlight selected chip
        switch (selected) {
            case 0:
                chipAll.setCardBackgroundColor(primaryColor);
                tvChipAll.setTextColor(onPrimaryColor);
                tvChipAll.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 1:
                chipUser.setCardBackgroundColor(primaryColor);
                tvChipUser.setTextColor(onPrimaryColor);
                tvChipUser.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 2:
                chipSystem.setCardBackgroundColor(primaryColor);
                tvChipSystem.setTextColor(onPrimaryColor);
                tvChipSystem.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }

    private void showTriggerEditDialog(InstalledApp app, AppTrigger existingTrigger) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_app_trigger, null);
        builder.setView(view);

        ImageView ivAppIcon = view.findViewById(R.id.iv_app_icon);
        TextView tvAppName = view.findViewById(R.id.tv_app_name);
        TextView tvPackageName = view.findViewById(R.id.tv_package_name);
        TextInputEditText etTrigger = view.findViewById(R.id.et_trigger);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        if (app != null) {
            ivAppIcon.setImageDrawable(app.getIcon());
            tvAppName.setText(app.getAppName());
            tvPackageName.setText(app.getPackageName());
            etTrigger.setText(AppTrigger.getDefaultTrigger(app.getAppName()));
            btnDelete.setVisibility(View.GONE);
        } else if (existingTrigger != null) {
            try {
                ivAppIcon.setImageDrawable(
                        requireContext().getPackageManager().getApplicationIcon(existingTrigger.getPackageName()));
            } catch (Exception e) {
                ivAppIcon.setImageResource(R.drawable.ic_cpu_filled);
            }
            tvAppName.setText(existingTrigger.getAppName());
            tvPackageName.setText(existingTrigger.getPackageName());
            etTrigger.setText(existingTrigger.getTrigger());
            btnDelete.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmationBottomSheet(existingTrigger);
        });

        btnSave.setOnClickListener(v -> {
            String trigger = etTrigger.getText().toString().trim();
            if (trigger.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a trigger", Toast.LENGTH_SHORT).show();
                return;
            }

            if (app != null) {
                AppTrigger newTrigger = new AppTrigger(
                        app.getPackageName(),
                        app.getActivityName(),
                        app.getAppName(),
                        trigger);
                manager.addTrigger(newTrigger);
            } else if (existingTrigger != null) {
                existingTrigger.setTrigger(trigger);
                manager.updateTrigger(existingTrigger);
            }

            dialog.dismiss();
            loadData();
        });

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showDeleteConfirmationBottomSheet(AppTrigger trigger) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_delete_app_trigger, null);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(view);

        ImageView ivAppIcon = view.findViewById(R.id.iv_app_icon);
        TextView tvAppName = view.findViewById(R.id.tv_app_name);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        tvAppName.setText(trigger.getAppName());
        try {
            Drawable icon = requireContext().getPackageManager().getApplicationIcon(trigger.getPackageName());
            ivAppIcon.setImageDrawable(icon);
        } catch (Exception e) {
            ivAppIcon.setImageResource(R.drawable.ic_cpu_filled);
        }

        btnDelete.setOnClickListener(v -> {
            manager.removeTrigger(trigger);
            dialog.dismiss();
            loadData();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onTriggerClick(AppTrigger trigger) {
        showTriggerEditDialog(null, trigger);
    }

    @Override
    public void onTriggerToggle(AppTrigger trigger, boolean enabled) {
        trigger.setEnabled(enabled);
        manager.updateTrigger(trigger);
    }

    private void applyAmoledIfNeeded(View view) {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(requireContext());
        boolean isAmoled = BottomSheetHelper.isAmoledMode(requireContext());

        if (isDarkMode && isAmoled) {
            View root = view.findViewById(R.id.root_layout);
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
        }
    }
}
