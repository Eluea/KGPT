package tn.eluea.kgpt.external;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.ui.UiInteractor;

/**
 * Floating dialog activity launched from Gboard via Xposed hook.
 * 
 * This activity uses ContentProvider as single source of truth.
 * Changes are saved directly to ContentProvider and are immediately
 * visible to both KGPT app and Xposed module.
 */
public class DialogActivity extends Activity {
    private ConfigContainer mConfig;

    private DialogBoxManager mDialogManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DialogType dialogType = DialogType.valueOf(
                getIntent().getStringExtra(UiInteractor.EXTRA_DIALOG_TYPE));

        mConfig = new ConfigContainer();
        mConfig.initClient(this); // Initialize ContentProvider client
        
        Bundle inputBundle = getIntent().getExtras();
        mDialogManager = new DialogBoxManager(this, inputBundle, mConfig);
        mDialogManager.showDialog(dialogType);
    }
}
