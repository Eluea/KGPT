package tn.eluea.kgpt.core.ui.dialog.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.util.MaterialYouManager;

import com.google.android.material.color.MaterialColors;

/**
 * Utility class to uniformly handle Dialog UI creation, specifically for
 * creating harmonized Material You list items with correct tinting.
 */
public class DialogUiUtils {

    /**
     * Adds a standardized settings option row to a container.
     * enforcing Material You colors programmatically to avoid theme inheritance
     * issues.
     *
     * @param container     The parent LinearLayout to add the view to.
     * @param title         The title text.
     * @param iconRes       The icon resource ID.
     * @param themedContext The context wrapped with correct Material You theme.
     * @param listener      The click listener.
     */
    public static void addSettingsOption(LinearLayout container, String title, int iconRes, Context themedContext,
            View.OnClickListener listener) {
        View itemView = LayoutInflater.from(themedContext).inflate(R.layout.item_settings_option, container, false);

        TextView tvTitle = itemView.findViewById(R.id.tv_title);
        ImageView ivIcon = itemView.findViewById(R.id.iv_icon);
        View iconContainer = itemView.findViewById(R.id.icon_container);

        ImageView ivArrow = itemView.findViewById(R.id.iv_arrow);

        tvTitle.setText(title);
        ivIcon.setImageResource(iconRes);

        // Enforce Colors
        applyMaterialYouTints(themedContext, ivIcon, iconContainer);
        // Also apply tint to the arrow
        if (ivArrow != null) {
            applyMaterialYouTints(themedContext, ivArrow, null);
        }

        itemView.setOnClickListener(listener);
        container.addView(itemView);
    }

    /**
     * Extracts dynamic colors from the context and applies them to the Icon and its
     * Container.
     */
    public static void applyMaterialYouTints(Context context, ImageView icon, View container) {
        // Use the passed context directly. It should already be wrapped by
        // FloatingBottomSheet/MaterialYouManager.
        Context dynamicContext = context;

        // Resolve Dynamic Primary Color
        int colorPrimary;
        try {
            TypedValue tv = new TypedValue();
            if (dynamicContext.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) {
                colorPrimary = tv.data;
            } else {
                // Fallback to seed color
                colorPrimary = MaterialYouManager.getInstance(context).getSeedColor();
            }
        } catch (Exception e) {
            colorPrimary = context.getResources().getColor(R.color.primary, context.getTheme());
        }

        // Resolve Dynamic Primary Container Color
        int colorContainer;
        try {
            TypedValue tv = new TypedValue();
            if (dynamicContext.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, tv,
                    true)) {
                colorContainer = tv.data;
            } else {
                // Fallback: generate light tint from seed color
                int seedColor = MaterialYouManager.getInstance(context).getSeedColor();
                colorContainer = generateLightTint(seedColor, context);
            }
        } catch (Exception e) {
            colorContainer = context.getResources().getColor(R.color.primary_light, context.getTheme());
        }

        if (icon != null) {
            icon.setColorFilter(colorPrimary);
        }
        if (container != null) {
            container.setBackgroundTintList(ColorStateList.valueOf(colorContainer));
        }
    }

    /**
     * Generates a lighter tint of the given color for container backgrounds.
     * In light mode, creates a very light pastel version.
     * In dark mode, creates a darker muted version.
     */
    private static int generateLightTint(int color, Context context) {
        float[] hsl = new float[3];
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Convert RGB to HSL
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float l = (max + min) / 2f;
        float s = 0f;
        float h = 0f;

        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);

            if (max == rf) {
                h = (gf - bf) / d + (gf < bf ? 6f : 0f);
            } else if (max == gf) {
                h = (bf - rf) / d + 2f;
            } else {
                h = (rf - gf) / d + 4f;
            }
            h /= 6f;
        }

        // Check night mode
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            // Dark mode: create a darker, muted version (like colorPrimaryContainer in dark
            // theme)
            s = Math.min(s * 0.6f, 0.4f); // Reduce saturation
            l = 0.25f; // Low lightness for dark mode
        } else {
            // Light mode: create a very light pastel version
            s = Math.min(s * 0.4f, 0.3f); // Low saturation for pastel look
            l = 0.92f; // Very high lightness for light tint
        }

        // Convert HSL back to RGB
        return hslToColor(h, s, l);
    }

    /**
     * Converts HSL values to an Android color int.
     */
    private static int hslToColor(float h, float s, float l) {
        float r, g, b;

        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1f + s) : l + s - l * s;
            float p = 2f * l - q;
            r = hueToRgb(p, q, h + 1f / 3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f / 3f);
        }

        return Color.rgb((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    private static float hueToRgb(float p, float q, float t) {
        if (t < 0f)
            t += 1f;
        if (t > 1f)
            t -= 1f;
        if (t < 1f / 6f)
            return p + (q - p) * 6f * t;
        if (t < 1f / 2f)
            return q;
        if (t < 2f / 3f)
            return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }

    /**
     * Applies the dialog background color to the root view.
     * It relies on the XML attribute ?attr/colorSurfaceContainer which should be
     * resolved correctly
     * by the themed context.
     *
     * @param context  The themed context used to inflate the view.
     * @param rootView The root view of the dialog layout.
     */
    /**
     * Applies the dialog background color to the root view.
     * It relies on the XML attribute ?attr/colorSurfaceContainer which should be
     * resolved correctly by the themed context.
     *
     * @param context  The themed context used to inflate the view.
     * @param rootView The root view of the dialog layout.
     */
    public static void applyDialogBackground(Context context, View rootView) {
        if (rootView == null)
            return;

        int backgroundColor;
        boolean isAmoled = false;

        // Check preference first to be sure
        try {
            android.content.SharedPreferences appPrefs = context.getSharedPreferences("keyboard_gpt_ui",
                    Context.MODE_PRIVATE);
            isAmoled = appPrefs.getBoolean("amoled_mode", false);
        } catch (Exception e) {
        }

        // Also check if context theme resolves colorSurface to BLACK, which is a strong
        // indicator of AMOLED overlay
        if (!isAmoled) {
            try {
                TypedValue typedValue = new TypedValue();
                if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
                    if (typedValue.data == Color.BLACK) {
                        isAmoled = true;
                    }
                }
            } catch (Exception e) {
            }
        }

        if (isAmoled) {
            backgroundColor = Color.BLACK;
        } else {
            // Not AMOLED. Check Night Mode to decide between our forced Dark Blue-Grey
            // (#1C2026) and Standard Light.

            int nightModeFlags = context.getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK;

            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // FORCE Dark Blue-Grey (#1C2026) for non-AMOLED Dark Mode
                backgroundColor = context.getColor(R.color.container_background_dark);
            } else {
                // Light Mode -> Use Light Background
                backgroundColor = context.getColor(R.color.container_background_light);
            }
        }

        rootView.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
    }

    /**
     * Programmatically forces the MaterialSwitch to use the blue color scheme
     * (primary color) for the thumb and track when checked.
     * 
     * @param context    The context to retrieve colors.
     * @param switchView The switch to style.
     */
    /**
     * Programmatically forces the MaterialSwitch to use the dynamic color scheme
     * (primary color) for the thumb and track when checked.
     * 
     * @param context    The context to retrieve colors.
     * @param switchView The switch to style.
     */
    public static void applySwitchTheme(Context context,
            com.google.android.material.materialswitch.MaterialSwitch switchView) {
        if (switchView == null)
            return;

        int colorPrimary;
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) {
                colorPrimary = tv.data;
            } else {
                colorPrimary = context.getColor(R.color.primary);
            }
        } catch (Exception e) {
            colorPrimary = context.getColor(R.color.primary);
        }

        int colorSurface = context.getColor(R.color.surface_dark); // Or track unchecked
        int colorUncheckedThumb = context.getColor(R.color.switch_thumb_unchecked);
        int colorUncheckedTrack = context.getColor(R.color.switch_track_unchecked);

        // Thumb tints
        // Checked: White (usually)
        // Unchecked: Grey
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] thumbColors = new int[] {
                Color.WHITE,
                colorUncheckedThumb
        };

        int[] trackColors = new int[] {
                colorPrimary,
                colorUncheckedTrack
        };

        switchView.setThumbTintList(new ColorStateList(states, thumbColors));
        switchView.setTrackTintList(new ColorStateList(states, trackColors));
    }

    /**
     * Programmatically forces the MaterialButton to use the dynamic color scheme.
     * 
     * @param context The context to retrieve colors.
     * @param button  The button to style.
     */
    public static void applyButtonTheme(Context context, com.google.android.material.button.MaterialButton button) {
        if (button == null)
            return;

        int colorPrimary;
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) {
                colorPrimary = tv.data;
            } else {
                colorPrimary = context.getColor(R.color.primary);
            }
        } catch (Exception e) {
            colorPrimary = context.getColor(R.color.primary);
        }

        button.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
        // Use OnPrimary for text color to ensure contrast
        int colorOnPrimary = Color.WHITE;
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tv, true)) {
                colorOnPrimary = tv.data;
            }
        } catch (Exception ignored) {
        }

        button.setTextColor(colorOnPrimary);
    }

    /**
     * Programmatically forces the RadioButton to use the dynamic color scheme.
     * 
     * @param context     The context to retrieve colors.
     * @param radioButton The radioButton to style.
     */
    public static void applyRadioButtonTheme(Context context, android.widget.RadioButton radioButton) {
        if (radioButton == null)
            return;

        radioButton.setButtonTintList(getCheckboxColorStateList(context));
    }

    public static ColorStateList getCheckboxColorStateList(Context context) {
        int colorPrimary = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary,
                Color.BLACK);
        int colorUnchecked = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline,
                Color.GRAY);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // checked
                new int[] { -android.R.attr.state_checked } // unchecked
        };

        int[] colors = new int[] {
                colorPrimary,
                colorUnchecked
        };

        return new ColorStateList(states, colors);
    }
}
