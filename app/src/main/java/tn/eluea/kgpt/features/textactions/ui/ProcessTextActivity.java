package tn.eluea.kgpt.features.textactions.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Entry point for ACTION_PROCESS_TEXT intent.
 * Simply forwards to TextActionsMenuActivity which handles everything.
 * 
 * For PROCESS_TEXT, we use startActivityForResult to get the result back
 * and return it to the calling app via setResult().
 */
public class ProcessTextActivity extends Activity {

    private static final String TAG = "KGPT_ProcessText";
    private static final int REQUEST_TEXT_ACTION = 1001;
    
    public static final String EXTRA_RESULT_TEXT = "result_text";

    private boolean isReadonly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Action: " + action);

        if (Intent.ACTION_PROCESS_TEXT.equals(action)) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            isReadonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);

            if (text == null || text.length() == 0) {
                Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String selectedText = text.toString();
            Log.d(TAG, "Text length: " + selectedText.length() + ", readonly: " + isReadonly);

            // Launch TextActionsMenuActivity for result
            Intent menuIntent = new Intent(this, TextActionsMenuActivity.class);
            menuIntent.putExtra(TextActionsMenuActivity.EXTRA_SELECTED_TEXT, selectedText);
            menuIntent.putExtra(TextActionsMenuActivity.EXTRA_READONLY, isReadonly);
            // Mark this as coming from PROCESS_TEXT so it returns result instead of broadcast
            menuIntent.putExtra("from_process_text", true);
            startActivityForResult(menuIntent, REQUEST_TEXT_ACTION);
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_TEXT_ACTION) {
            if (resultCode == RESULT_OK && data != null) {
                String resultText = data.getStringExtra(EXTRA_RESULT_TEXT);
                if (resultText != null && !isReadonly) {
                    // Return the result to the calling app
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, resultText);
                    setResult(RESULT_OK, resultIntent);
                    Log.d(TAG, "Returning result text: " + resultText.length() + " chars");
                }
            }
            finish();
        }
    }
}
