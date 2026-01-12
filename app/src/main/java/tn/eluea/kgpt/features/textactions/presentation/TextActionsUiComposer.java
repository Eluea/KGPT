package tn.eluea.kgpt.features.textactions.presentation;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.textactions.domain.TextAction;

public class TextActionsUiComposer {

    // Theme mode constants
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_AMOLED = 2;

    public interface UiActionListener {
        void onActionClicked(TextAction action);

        void onCustomActionClicked(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action);

        void onCloseClicked();

        void onBackgroundClicked();

        void onLanguageSelected(String language);

        void onCancelLanguageSelection();

        void onReplaceClicked();

        void onAppendClicked();

        void onCopyClicked();

        void onTranslateClicked();
    }

    private final Context context;
    private final UiActionListener listener;
    private final int currentTheme;

    private FrameLayout rootLayout;
    private LinearLayout menuCard;
    private LinearLayout cardContentContainer;
    private ValueAnimator loadingAnimator;

    public TextActionsUiComposer(Context context, UiActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentTheme = detectTheme(context);
    }

    /**
     * Detect current theme mode: Light, Dark, or AMOLED
     */
    private int detectTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = prefs.getBoolean("amoled_mode", false);
        
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        
        if (isDarkMode && isAmoled) {
            return THEME_AMOLED;
        } else if (isDarkMode) {
            return THEME_DARK;
        } else {
            return THEME_LIGHT;
        }
    }

    /**
     * Get background color based on current theme
     */
    private int getBackgroundColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
                return Color.BLACK;
            case THEME_DARK:
                return Color.parseColor("#1C2026"); // Dark blue-grey
            default: // THEME_LIGHT
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, typedValue, true);
                return typedValue.data;
        }
    }

    /**
     * Get text color based on current theme
     */
    private int getOnSurfaceColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
            case THEME_DARK:
                return Color.WHITE;
            default: // THEME_LIGHT
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                return typedValue.data;
        }
    }

    /**
     * Get secondary text color based on current theme
     */
    private int getOnSurfaceVariantColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
                return Color.parseColor("#B3FFFFFF"); // 70% white
            case THEME_DARK:
                return Color.parseColor("#B2BEC3"); // Light grey
            default: // THEME_LIGHT
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
                return typedValue.data;
        }
    }

    /**
     * Get divider/outline color based on current theme
     */
    private int getOutlineColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
                return Color.parseColor("#33FFFFFF"); // 20% white
            case THEME_DARK:
                return Color.parseColor("#3D4852"); // Dark grey
            default: // THEME_LIGHT
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true);
                return typedValue.data;
        }
    }

    /**
     * Get primary/accent color based on current theme
     */
    private int getPrimaryColor() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    /**
     * Get surface variant color for buttons
     */
    private int getSurfaceVariantColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
                return Color.parseColor("#1A1A1A"); // Very dark grey
            case THEME_DARK:
                return Color.parseColor("#2D3436"); // Dark grey
            default: // THEME_LIGHT
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true);
                return typedValue.data;
        }
    }

    /**
     * Get dim/scrim background color based on current theme
     */
    private int getScrimColor() {
        switch (currentTheme) {
            case THEME_AMOLED:
                return Color.parseColor("#CC000000"); // 80% black for AMOLED
            case THEME_DARK:
                return Color.parseColor("#99000000"); // 60% black for dark
            default: // THEME_LIGHT
                return Color.parseColor("#80000000"); // 50% black for light
        }
    }

    public void createUI(Activity activity) {
        rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(getScrimColor());
        rootLayout.setOnClickListener(v -> listener.onBackgroundClicked());

        // Main Card
        menuCard = new LinearLayout(context);
        menuCard.setOrientation(LinearLayout.VERTICAL);
        menuCard.setBackground(createCardBackground());
        menuCard.setElevation(dp(12));
        menuCard.setClickable(true);
        menuCard.setMinimumWidth(dp(200));

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        menuParams.gravity = Gravity.CENTER;
        int margin = dp(24);
        menuParams.setMargins(margin, 0, margin, 0);
        menuCard.setLayoutParams(menuParams);

        // Content Container
        cardContentContainer = new LinearLayout(context);
        cardContentContainer.setOrientation(LinearLayout.VERTICAL);
        menuCard.addView(cardContentContainer);

        rootLayout.addView(menuCard);
        activity.setContentView(rootLayout);
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(getBackgroundColor());
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), getOutlineColor());
        return background;
    }

    public void showMainMenu(List<TextAction> actions,
            List<tn.eluea.kgpt.features.textactions.domain.CustomTextAction> customActions,
            boolean showLabels) {
        cardContentContainer.removeAllViews();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout actionsContainer = new LinearLayout(context);
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.CENTER_VERTICAL);

        // Standard Actions
        for (TextAction action : actions) {
            actionsContainer.addView(createActionButton(action, showLabels));
            actionsContainer.addView(createVerticalDivider());
        }

        // Custom Actions
        for (tn.eluea.kgpt.features.textactions.domain.CustomTextAction action : customActions) {
            if (action.enabled) {
                actionsContainer.addView(createActionButton(action, showLabels));
                actionsContainer.addView(createVerticalDivider());
            }
        }

        actionsContainer.addView(createCloseButtonIcon());

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.addView(actionsContainer);

        container.addView(scrollView);
        cardContentContainer.addView(container);
    }

    public void animateIn() {
        if (menuCard == null)
            return;
        menuCard.setAlpha(0f);
        menuCard.setScaleX(0.8f);
        menuCard.setScaleY(0.8f);
        menuCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    public void animateOut(Runnable endAction) {
        if (menuCard == null) {
            if (endAction != null)
                endAction.run();
            return;
        }
        menuCard.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction(endAction)
                .start();
    }

    // ... (Adding other UI methods: showLoading, showResult, showLanguageSelector)

    private View createActionButton(TextAction action, boolean showLabel) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());

        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));

        try {
            icon.setImageResource(action.iconRes);
            icon.setColorFilter(getPrimaryColor());
        } catch (Exception e) {
            GradientDrawable fallback = new GradientDrawable();
            fallback.setShape(GradientDrawable.OVAL);
            fallback.setColor(getPrimaryColor());
            icon.setBackground(fallback);
        }
        button.addView(icon);

        if (showLabel) {
            TextView label = new TextView(context);
            label.setText(action.labelEn);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTextColor(getOnSurfaceColor());
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(4);
            label.setLayoutParams(labelParams);
            button.addView(label);
        }

        button.setOnClickListener(v -> listener.onActionClicked(action));
        return button;
    }

    private View createActionButton(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action,
            boolean showLabel) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());

        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));

        icon.setImageResource(R.drawable.ic_star_filled);
        icon.setColorFilter(getPrimaryColor());

        button.addView(icon);

        if (showLabel) {
            TextView label = new TextView(context);
            label.setText(action.name);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTextColor(getOnSurfaceColor());
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(4);
            label.setLayoutParams(labelParams);
            button.addView(label);
        }

        button.setOnClickListener(v -> listener.onCustomActionClicked(action));
        return button;
    }

    private View createCloseButtonIcon() {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());

        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        icon.setColorFilter(getOnSurfaceVariantColor());
        button.addView(icon);

        button.setOnClickListener(v -> listener.onCloseClicked());
        return button;
    }

    private View createVerticalDivider() {
        View divider = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(28));
        params.setMargins(dp(4), 0, dp(4), 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(getOutlineColor());
        return divider;
    }

    public void showLoading() {
        cardContentContainer.removeAllViews();

        LinearLayout loadingView = new LinearLayout(context);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setPadding(dp(24), dp(20), dp(24), dp(20));
        loadingView.setMinimumWidth(dp(200));

        TextView generatingText = new TextView(context);
        generatingText.setText("Generating...");
        generatingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        generatingText.setTextColor(getOnSurfaceColor());
        generatingText.setGravity(Gravity.CENTER);
        loadingView.addView(generatingText);

        cardContentContainer.addView(loadingView);

        generatingText.post(() -> startWaveAnimation(generatingText));
    }

    private void startWaveAnimation(TextView textView) {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }

        int width = textView.getWidth();
        if (width == 0)
            return;

        int dimColor = getOnSurfaceVariantColor();
        int brightColor = getOnSurfaceColor();

        Shader textShader = new LinearGradient(0, 0, width, 0,
                new int[] { dimColor, brightColor, dimColor },
                new float[] { 0, 0.5f, 1 },
                Shader.TileMode.CLAMP);
        textView.getPaint().setShader(textShader);

        Matrix matrix = new Matrix();
        loadingAnimator = ValueAnimator.ofFloat(-1f, 1f);
        loadingAnimator.setDuration(1500);
        loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingAnimator.setInterpolator(new LinearInterpolator());
        loadingAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            matrix.setTranslate(width * value * 2, 0);
            textShader.setLocalMatrix(matrix);
            textView.invalidate();
        });
        loadingAnimator.start();
    }

    public void cancelLoading() {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
            loadingAnimator = null;
        }
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    private int getSelectableItemBackground() {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        return outValue.resourceId;
    }

    public void showResult(String result, boolean isReadonly) {
        cancelLoading();
        cardContentContainer.removeAllViews();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        ScrollView textScroll = new ScrollView(context);
        textScroll.setFillViewport(true);

        TextView resultText = new TextView(context);
        resultText.setText(result);
        resultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        resultText.setTextColor(getOnSurfaceColor());
        resultText.setLineSpacing(dp(4), 1.0f);
        resultText.setTextIsSelectable(true);
        textScroll.addView(resultText);

        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int maxHeight = (int) (screenHeight * 0.45);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.weight = 1.0f;
        container.addView(textScroll, scrollParams);

        textScroll.post(() -> {
            if (textScroll.getHeight() > maxHeight) {
                ViewGroup.LayoutParams lp = textScroll.getLayoutParams();
                lp.height = maxHeight;
                textScroll.setLayoutParams(lp);
            }
        });

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(getOutlineColor());
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, dp(12), 0, dp(12));
        container.addView(divider, divParams);

        // Buttons
        // Row 1
        if (!isReadonly) {
            LinearLayout row1 = new LinearLayout(context);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER);

            row1.addView(createButton("Replace", v -> listener.onReplaceClicked()),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            View s1 = new View(context);
            row1.addView(s1, new LinearLayout.LayoutParams(dp(8), dp(1)));

            row1.addView(createButton("Append", v -> listener.onAppendClicked()),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            container.addView(row1);

            View sRow = new View(context);
            container.addView(sRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        }

        // Row 2
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        row2.addView(createButton("Translate", v -> listener.onTranslateClicked()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        View s2 = new View(context);
        row2.addView(s2, new LinearLayout.LayoutParams(dp(8), dp(1)));

        row2.addView(createButton("Copy", v -> listener.onCopyClicked()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        container.addView(row2);

        // Spacer
        View sRow2 = new View(context);
        container.addView(sRow2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        // Row 3: Close
        LinearLayout row3 = new LinearLayout(context);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.CENTER);
        row3.addView(createButton("Close", v -> listener.onCloseClicked()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        container.addView(row3);

        cardContentContainer.addView(container);
    }

    public void showLanguageSelector(List<String> languages) {
        cardContentContainer.removeAllViews();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setMinimumWidth(dp(250));
        container.setPadding(dp(0), dp(8), dp(0), dp(8));

        TextView header = new TextView(context);
        header.setText("Select Language");
        header.setTextColor(getOnSurfaceColor());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(8), 0, dp(12));
        container.addView(header);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);

        for (String lang : languages) {
            TextView langItem = new TextView(context);
            langItem.setText(lang);
            langItem.setTextColor(getOnSurfaceColor());
            langItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            langItem.setPadding(dp(24), dp(12), dp(24), dp(12));
            langItem.setBackgroundResource(getSelectableItemBackground());
            langItem.setClickable(true);
            langItem.setOnClickListener(v -> listener.onLanguageSelected(lang));
            list.addView(langItem);
        }

        scrollView.addView(list);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300));
        container.addView(scrollView, scrollParams);

        View divider = new View(context);
        divider.setBackgroundColor(getOutlineColor());
        container.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(getOnSurfaceVariantColor());
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setPadding(0, dp(12), 0, dp(8));
        cancelBtn.setClickable(true);
        cancelBtn.setBackgroundResource(getSelectableItemBackground());
        cancelBtn.setOnClickListener(v -> listener.onCancelLanguageSelection());
        container.addView(cancelBtn);

        cardContentContainer.addView(container);
    }

    private View createButton(String text, View.OnClickListener listener) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextColor(getOnSurfaceColor());
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getSurfaceVariantColor());
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), getOutlineColor());
        btn.setBackground(bg);

        btn.setClickable(true);
        btn.setOnClickListener(listener);
        return btn;
    }
}
