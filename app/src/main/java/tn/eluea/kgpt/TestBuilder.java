package tn.eluea.kgpt;

import com.google.android.material.color.DynamicColorsOptions;

public class TestBuilder {
    public void test() {
        DynamicColorsOptions.Builder builder = new DynamicColorsOptions.Builder();
        builder.setContentBasedSource(0xFF00FF00);
    }
}
