package tn.eluea.kgpt.external;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Entry point for ACTION_PROCESS_TEXT intent.
 * Simply forwards to TextActionsMenuActivity which handles everything.
 */
public class ProcessTextActivity extends Activity {
    
    private static final String TAG = "KGPT_ProcessText";
    
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
            boolean readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);
            
            if (text == null || text.length() == 0) {
                Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            String selectedText = text.toString();
            Log.d(TAG, "Text length: " + selectedText.length() + ", readonly: " + readonly);
            
            // Launch TextActionsMenuActivity directly
            Intent menuIntent = new Intent(this, TextActionsMenuActivity.class);
            menuIntent.putExtra(TextActionsMenuActivity.EXTRA_SELECTED_TEXT, selectedText);
            menuIntent.putExtra(TextActionsMenuActivity.EXTRA_READONLY, readonly);
            startActivity(menuIntent);
            
            // Finish this activity immediately
            finish();
        } else {
            finish();
        }
    }
}
