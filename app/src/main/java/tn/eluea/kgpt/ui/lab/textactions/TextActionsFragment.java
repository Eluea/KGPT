package tn.eluea.kgpt.ui.lab.textactions;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

import java.util.HashSet;
import java.util.Set;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.data.TextActionManager;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.MainActivity;

public class TextActionsFragment extends Fragment {

    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final String PREF_TEXT_ACTIONS_LIST = "text_actions_list";
    private static final String PREF_TEXT_ACTIONS_SHOW_LABELS = "text_actions_show_labels";

    private ConfigClient configClient;
    private MaterialSwitch switchEnabled;
    private MaterialSwitch switchShowLabels;
    private LinearLayout actionsListLayout;
    private View contentContainer;
    private View emptyState;
    private Set<TextAction> enabledActions = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configClient = new ConfigClient(requireContext());
        initViews(view);
        loadSettings();
        applyAmoledIfNeeded(view);

        // Update Dock Action
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setDockAction("Add Custom Action", R.drawable.ic_add, v -> {
                showAddCustomActionDialog(new TextActionManager(requireContext()));
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

        switchEnabled = view.findViewById(R.id.switch_enabled);
        switchShowLabels = view.findViewById(R.id.switch_show_labels);
        actionsListLayout = view.findViewById(R.id.actions_list_layout);
        contentContainer = view.findViewById(R.id.content_container);
        emptyState = view.findViewById(R.id.empty_state);

        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showRestartRequiredDialog(isChecked);
        });
    }

    private void loadSettings() {
        boolean isEnabled = configClient.getBoolean(PREF_TEXT_ACTIONS_ENABLED, false);
        boolean showLabels = configClient.getBoolean(PREF_TEXT_ACTIONS_SHOW_LABELS, true);
        String actionsJson = configClient.getString(PREF_TEXT_ACTIONS_LIST, null);

        switchEnabled.setOnCheckedChangeListener(null);
        switchEnabled.setChecked(isEnabled);
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showRestartRequiredDialog(isChecked);
        });

        switchShowLabels.setOnCheckedChangeListener(null);
        switchShowLabels.setChecked(showLabels);
        switchShowLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configClient.putBoolean(PREF_TEXT_ACTIONS_SHOW_LABELS, isChecked);
            showSaveConfirmation();
        });

        updateContentVisibility(isEnabled);

        enabledActions = decodeEnabledActions(actionsJson);
        if (enabledActions.isEmpty()) {
            for (TextAction a : TextAction.values())
                enabledActions.add(a);
        }

        populateActionsList();
    }

    private void populateActionsList() {
        if (getContext() == null)
            return;
        actionsListLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        TextActionManager actionManager = new TextActionManager(requireContext());

        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(actionsListLayout,
                androidx.appcompat.R.attr.colorPrimary);
        int colorSecondary = com.google.android.material.color.MaterialColors.getColor(actionsListLayout,
                com.google.android.material.R.attr.colorSecondary);

        int index = 0;
        for (TextAction action : TextAction.values()) {
            View view = inflater.inflate(R.layout.item_text_action_setting, actionsListLayout, false);

            ImageView icon = view.findViewById(R.id.action_icon);
            TextView title = view.findViewById(R.id.action_title);
            TextView subtitle = view.findViewById(R.id.action_subtitle);
            MaterialSwitch switchAction = view.findViewById(R.id.switch_action);

            icon.setImageResource(action.iconRes);

            int color;
            int mod = index % 2;
            if (mod == 0)
                color = colorPrimary;
            else
                color = colorSecondary;

            icon.setColorFilter(color);
            index++;

            title.setText(action.labelEn);
            subtitle.setText(action.getLabel(false));

            boolean isActionEnabled = enabledActions.contains(action);
            switchAction.setChecked(isActionEnabled);

            switchAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enabledActions.add(action);
                } else {
                    enabledActions.remove(action);
                }
                saveEnabledActions();
                showSaveConfirmation();
            });

            view.setOnClickListener(v -> showEditPromptDialog(action, actionManager));

            actionsListLayout.addView(view);
        }

        // Custom Actions
        java.util.List<tn.eluea.kgpt.features.textactions.domain.CustomTextAction> customActions = actionManager
                .getCustomActions();
        for (tn.eluea.kgpt.features.textactions.domain.CustomTextAction action : customActions) {
            View view = inflater.inflate(R.layout.item_text_action_setting, actionsListLayout, false);

            ImageView icon = view.findViewById(R.id.action_icon);
            TextView title = view.findViewById(R.id.action_title);
            TextView subtitle = view.findViewById(R.id.action_subtitle);
            MaterialSwitch switchAction = view.findViewById(R.id.switch_action);

            icon.setImageResource(R.drawable.ic_star_filled);
            icon.setColorFilter(colorPrimary);

            title.setText(action.name);
            subtitle.setText("Custom Action");

            switchAction.setChecked(action.enabled);

            switchAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                action.enabled = isChecked;
                actionManager.updateCustomAction(action);
            });

            view.setOnClickListener(v -> showEditCustomActionDialog(action, actionManager));

            actionsListLayout.addView(view);
        }
    }

    private void showAddCustomActionDialog(TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                requireContext(),
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_custom_text_action, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.et_name);
        com.google.android.material.textfield.TextInputEditText etPrompt = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String prompt = etPrompt.getText().toString().trim();

            if (name.isEmpty() || prompt.isEmpty()) {
                if (getView() != null)
                    Snackbar.make(getView(), "Please enter name and prompt", Snackbar.LENGTH_SHORT).show();
                return;
            }

            actionManager.addCustomAction(name, prompt);
            populateActionsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showEditCustomActionDialog(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action,
            TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                requireContext(),
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_custom_text_action, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.et_name);
        com.google.android.material.textfield.TextInputEditText etPrompt = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);
        com.google.android.material.button.MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        etName.setText(action.name);
        etPrompt.setText(action.prompt);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String prompt = etPrompt.getText().toString().trim();

            if (name.isEmpty() || prompt.isEmpty()) {
                if (getView() != null)
                    Snackbar.make(getView(), "Please enter name and prompt", Snackbar.LENGTH_SHORT).show();
                return;
            }

            action.name = name;
            action.prompt = prompt;
            actionManager.updateCustomAction(action);
            populateActionsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            showDeleteCustomActionBottomSheet(action, actionManager, dialog);
        });

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showDeleteCustomActionBottomSheet(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action,
            TextActionManager actionManager,
            androidx.appcompat.app.AlertDialog editDialog) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_delete_custom_action, null);

        FloatingBottomSheet bottomSheet = new FloatingBottomSheet(requireContext());
        bottomSheet.setContentView(view);

        TextView tvActionName = view.findViewById(R.id.tv_action_name);
        com.google.android.material.button.MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        tvActionName.setText(action.name);

        btnDelete.setOnClickListener(v -> {
            actionManager.deleteCustomAction(action.id);
            bottomSheet.dismiss();
            editDialog.dismiss();
            populateActionsList();
        });

        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void showEditPromptDialog(TextAction action, TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                requireContext(),
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text_action_prompt, null);
        builder.setView(view);

        ImageView icon = view.findViewById(R.id.iv_action_icon);
        TextView title = view.findViewById(R.id.tv_title);
        com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnReset = view.findViewById(R.id.btn_reset);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);

        icon.setImageResource(action.iconRes);
        try {
            icon.setColorFilter(Color.parseColor(action.color));
        } catch (Exception e) {
            icon.setColorFilter(Color.WHITE);
        }

        title.setText("Edit " + action.labelEn + " Prompt");
        input.setText(actionManager.getActionPrompt(action));

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String newPrompt = input.getText().toString().trim();
            if (!newPrompt.isEmpty()) {
                actionManager.setActionPrompt(action, newPrompt);
            }
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            actionManager.resetActionPrompt(action);
            input.setText(actionManager.getActionPrompt(action));
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void updateContentVisibility(boolean enabled) {
        contentContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private void saveEnabledActions() {
        String json = TextActionManager.encodeEnabledActions(enabledActions);
        configClient.putString(PREF_TEXT_ACTIONS_LIST, json);
    }

    private Set<TextAction> decodeEnabledActions(String json) {
        Set<TextAction> actions = new HashSet<>();
        if (json == null || json.isEmpty()) {
            return actions;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String actionName = array.getString(i);
                try {
                    actions.add(TextAction.valueOf(actionName));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actions;
    }

    private void showSaveConfirmation() {
        // Silent save
    }

    private void applyAmoledIfNeeded(View view) {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(requireContext());
        boolean isAmoled = BottomSheetHelper.isAmoledMode(requireContext());

        if (isDarkMode && isAmoled) {
            View root = view.findViewById(R.id.root_layout);
            if (root != null)
                root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
        }
    }

    private void showRestartRequiredDialog(boolean newEnabledState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_restart_required, null);

        final tn.eluea.kgpt.ui.main.FloatingBottomSheet bottomSheetDialog = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                requireContext());
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);

        View btnRestartContainer = view.findViewById(R.id.btn_restart_container);
        android.widget.TextView tvTag = view.findViewById(R.id.tv_countdown_tag);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        if (tvTag != null)
            tvTag.setText("6s");

        android.os.CountDownTimer timer = new android.os.CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvTag != null && bottomSheetDialog.isShowing()) {
                    tvTag.setText((millisUntilFinished / 1000 + 1) + "s");
                }
            }

            @Override
            public void onFinish() {
                if (bottomSheetDialog.isShowing()) {
                    performRestart(newEnabledState);
                    bottomSheetDialog.dismiss();
                }
            }
        };
        timer.start();

        if (btnRestartContainer != null) {
            btnRestartContainer.setOnClickListener(v -> {
                timer.cancel();
                performRestart(newEnabledState);
                bottomSheetDialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                timer.cancel();
                switchEnabled.setOnCheckedChangeListener(null);
                switchEnabled.setChecked(!newEnabledState);
                switchEnabled
                        .setOnCheckedChangeListener((buttonView, isChecked) -> showRestartRequiredDialog(isChecked));
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void performRestart(boolean newEnabledState) {
        configClient.putBoolean(PREF_TEXT_ACTIONS_ENABLED, newEnabledState);
        updateContentVisibility(newEnabledState);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            tn.eluea.kgpt.KGPTApplication.restartApp(requireActivity(), MainActivity.class);
        }, 300);
    }
}
