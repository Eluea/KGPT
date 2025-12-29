package tn.eluea.kgpt.ui.main.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.backup.BackupManager;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class SettingsFragment extends Fragment {

    private static final String PREF_THEME = "theme_mode";
    private static final String PREF_AMOLED = "amoled_mode";

    private MaterialSwitch switchDarkMode, switchAmoled, switchLogs, switchExternalInternet;
    private LinearLayout amoledContainer, btnBackup, btnRestore;
    private TextView tvAboutVersion;
    private FrameLayout btnInfo;
    private MaterialButton btnTelegramSupport, btnChangelog;
    private View rootView;

    private SharedPreferences uiPrefs;
    private BackupManager backupManager;
    
    // Activity result launchers for file picker
    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize backup launcher
        backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performBackup(uri);
                    }
                }
            }
        );
        
        // Initialize restore launcher
        restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performRestore(uri);
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        uiPrefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        backupManager = new BackupManager(requireContext());
        initViews(view);
        applyAmoledIfNeeded();
        applyThemeColors();
        loadSettings();
        setupListeners();
    }

    private void initViews(View view) {
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchAmoled = view.findViewById(R.id.switch_amoled);
        switchLogs = view.findViewById(R.id.switch_logs);
        switchExternalInternet = view.findViewById(R.id.switch_external_internet);
        amoledContainer = view.findViewById(R.id.amoled_container);
        tvAboutVersion = view.findViewById(R.id.tv_about_version);
        btnInfo = view.findViewById(R.id.btn_info);
        btnTelegramSupport = view.findViewById(R.id.btn_telegram_support);
        btnChangelog = view.findViewById(R.id.btn_changelog);
        btnBackup = view.findViewById(R.id.btn_backup);
        btnRestore = view.findViewById(R.id.btn_restore);
    }

    private void applyThemeColors() {
        // Apply theme color to switches with better visibility
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int trackCheckedColor = ContextCompat.getColor(requireContext(), R.color.switch_track_checked);
        int trackUncheckedColor = ContextCompat.getColor(requireContext(), R.color.switch_track_unchecked);
        int thumbCheckedColor = ContextCompat.getColor(requireContext(), R.color.switch_thumb_checked);
        int thumbUncheckedColor = ContextCompat.getColor(requireContext(), R.color.switch_thumb_unchecked);
        
        applySwitchColors(switchDarkMode, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
        applySwitchColors(switchAmoled, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
        applySwitchColors(switchLogs, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
        applySwitchColors(switchExternalInternet, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
    }

    private void applySwitchColors(MaterialSwitch switchView, int thumbChecked, int thumbUnchecked, int trackChecked, int trackUnchecked) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] { -android.R.attr.state_checked }
        };
        
        int[] thumbColors = new int[] { thumbChecked, thumbUnchecked };
        int[] trackColors = new int[] { trackChecked, trackUnchecked };
        
        switchView.setThumbTintList(new ColorStateList(states, thumbColors));
        switchView.setTrackTintList(new ColorStateList(states, trackColors));
    }

    private ColorStateList createSwitchThumbColorStateList(int checkedColor) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
            checkedColor,
            ContextCompat.getColor(requireContext(), R.color.text_secondary)
        };
        return new ColorStateList(states, colors);
    }

    private void applyAmoledIfNeeded() {
        boolean isAmoled = uiPrefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = uiPrefs.getBoolean(PREF_THEME, false);
        
        if (isDarkMode && isAmoled) {
            // Apply AMOLED colors
            rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
            
            // Update all cards to AMOLED surface color
            applyAmoledToCards(rootView);
        }
    }

    private void applyAmoledToCards(View view) {
        if (view instanceof MaterialCardView) {
            ((MaterialCardView) view).setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.surface_amoled)
            );
            ((MaterialCardView) view).setStrokeColor(
                ContextCompat.getColor(requireContext(), R.color.divider_dark)
            );
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    private void loadSettings() {
        // UI Settings
        boolean isDarkMode = uiPrefs.getBoolean(PREF_THEME, false);
        boolean isAmoled = uiPrefs.getBoolean(PREF_AMOLED, false);
        
        switchDarkMode.setChecked(isDarkMode);
        switchAmoled.setChecked(isAmoled);
        
        // AMOLED option only available when dark mode is enabled
        amoledContainer.setAlpha(isDarkMode ? 1.0f : 0.5f);
        switchAmoled.setEnabled(isDarkMode);

        // App Settings
        if (SPManager.isReady()) {
            switchLogs.setChecked(SPManager.getInstance().getEnableLogs());
            switchExternalInternet.setChecked(SPManager.getInstance().getEnableExternalInternet());
        }

        // Version
        tvAboutVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }

    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            uiPrefs.edit().putBoolean(PREF_THEME, isChecked).apply();
            
            // Update AMOLED option availability
            amoledContainer.setAlpha(isChecked ? 1.0f : 0.5f);
            switchAmoled.setEnabled(isChecked);
            
            if (!isChecked && switchAmoled.isChecked()) {
                switchAmoled.setChecked(false);
                uiPrefs.edit().putBoolean(PREF_AMOLED, false).apply();
            }
            
            // Apply theme change
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            
            // Recreate to apply theme
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        switchAmoled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!switchAmoled.isEnabled()) return;
            
            uiPrefs.edit().putBoolean(PREF_AMOLED, isChecked).apply();
            // Recreate activity to apply AMOLED theme
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        switchLogs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SPManager.isReady()) {
                SPManager.getInstance().setOtherSetting(OtherSettingsType.EnableLogs, isChecked);
            }
        });

        switchExternalInternet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SPManager.isReady()) {
                SPManager.getInstance().setOtherSetting(OtherSettingsType.EnableExternalInternet, isChecked);
            }
        });

        btnInfo.setOnClickListener(v -> showInfoBottomSheet());

        btnTelegramSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SupKGPT"));
            startActivity(intent);
        });

        btnChangelog.setOnClickListener(v -> showChangelogBottomSheet());

        btnBackup.setOnClickListener(v -> startBackup());
        btnRestore.setOnClickListener(v -> startRestore());
    }

    private void startBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, BackupManager.generateBackupFilename());
        backupLauncher.launch(intent);
    }

    private void startRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }

    private void performBackup(Uri uri) {
        try {
            String backupJson = backupManager.createBackup();
            boolean success = backupManager.saveToFile(uri, backupJson);
            
            if (success) {
                Toast.makeText(requireContext(), "Backup saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to save backup", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error creating backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performRestore(Uri uri) {
        String backupJson = backupManager.readFromFile(uri);
        
        if (backupJson == null) {
            Toast.makeText(requireContext(), "Failed to read backup file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BackupManager.RestoreResult result = backupManager.restoreBackup(backupJson);
        
        if (result.success) {
            Toast.makeText(requireContext(), "Restored " + result.itemsRestored + " settings. Restart app to apply theme changes.", Toast.LENGTH_LONG).show();
            // Reload settings
            loadSettings();
        } else {
            Toast.makeText(requireContext(), "Restore failed: " + result.errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangelogBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_changelog, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Set version
        TextView tvVersion = sheetView.findViewById(R.id.tv_version);
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);

        // Add changelog entries
        LinearLayout changelogContent = sheetView.findViewById(R.id.changelog_content);
        
        // ADD entries
        addChangelogEntry(changelogContent, "ADD", "Added /ask command for inline AI queries");
        addChangelogEntry(changelogContent, "ADD", "Added Lab section for experimental features");
        addChangelogEntry(changelogContent, "ADD", "Added Telegram Support Group button");
        addChangelogEntry(changelogContent, "ADD", "Added Changelog button in Settings");
        addChangelogEntry(changelogContent, "ADD", "Added Backup & Restore (API keys not saved for security)");
        addChangelogEntry(changelogContent, "ADD", "Added ContentProvider for unified config sync");
        addChangelogEntry(changelogContent, "ADD", "Added unlimited custom commands");
        
        // IMPROVE entries
        addChangelogEntry(changelogContent, "IMPROVE", "Improved trigger and command cards design");
        addChangelogEntry(changelogContent, "IMPROVE", "Improved examples to be dynamic based on settings");
        addChangelogEntry(changelogContent, "IMPROVE", "Improved Quick Guide with icons");
        addChangelogEntry(changelogContent, "IMPROVE", "Improved config sync architecture");
        addChangelogEntry(changelogContent, "IMPROVE", "Improved dialog button styles");
        
        // ADJUST entries
        addChangelogEntry(changelogContent, "ADJUST", "Changed package name to tn.eluea.kgpt");
        
        // REMOVE entries
        addChangelogEntry(changelogContent, "REMOVE", "Removed Reset buttons from pattern items");
        
        // FIX entries
        addChangelogEntry(changelogContent, "FIX", "Fixed dock crash when switching theme");
        addChangelogEntry(changelogContent, "FIX", "Fixed config sync between app and dialogs");

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addChangelogEntry(LinearLayout container, String tag, String text) {
        View entryView = LayoutInflater.from(requireContext()).inflate(R.layout.item_changelog_entry, container, false);
        
        TextView tvTag = entryView.findViewById(R.id.tv_tag);
        TextView tvText = entryView.findViewById(R.id.tv_text);
        
        tvTag.setText(tag);
        tvText.setText(text);
        
        // Set tag background based on type
        int bgRes;
        switch (tag) {
            case "ADD":
                bgRes = R.drawable.bg_tag_add;
                break;
            case "IMPROVE":
                bgRes = R.drawable.bg_tag_improve;
                break;
            case "ADJUST":
                bgRes = R.drawable.bg_tag_adjust;
                break;
            case "REMOVE":
                bgRes = R.drawable.bg_tag_remove;
                break;
            case "FIX":
                bgRes = R.drawable.bg_tag_fix;
                break;
            default:
                bgRes = R.drawable.bg_tag_add;
        }
        tvTag.setBackgroundResource(bgRes);
        
        container.addView(entryView);
    }

    private void showInfoBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_info, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Set content
        TextView tvTitle = sheetView.findViewById(R.id.tv_info_title);
        TextView tvDescription = sheetView.findViewById(R.id.tv_info_description);
        
        tvTitle.setText("About Settings");
        tvDescription.setText("Customize your KGPT experience.\n\n" +
                "• Dark Mode: Enable dark theme for comfortable viewing at night.\n\n" +
                "• AMOLED Dark: Pure black theme for OLED screens to save battery.\n\n" +
                "• Enable Logging: Turn on logs for debugging. Disable for better performance.\n\n" +
                "• External Internet Service: Recommended for chat completion. Enables external network requests for AI responses.");

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
