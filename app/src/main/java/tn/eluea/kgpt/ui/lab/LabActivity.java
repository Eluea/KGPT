package tn.eluea.kgpt.ui.lab;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;

public class LabActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        applyTheme();
    }
    
    private void applyTheme() {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(this);
        boolean isAmoled = BottomSheetHelper.isAmoledMode(this);
        
        if (isDarkMode) {
            getWindow().getDecorView().setSystemUiVisibility(0);
            View root = findViewById(R.id.root_layout);
            if (isAmoled) {
                root.setBackgroundColor(ContextCompat.getColor(this, R.color.background_amoled));
            } else {
                root.setBackgroundColor(ContextCompat.getColor(this, R.color.background_dark));
            }
        }
    }
}
