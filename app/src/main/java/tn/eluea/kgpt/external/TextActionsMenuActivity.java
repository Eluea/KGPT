/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.external;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.textactions.SelectionHandler;
import tn.eluea.kgpt.textactions.TextAction;
import tn.eluea.kgpt.textactions.TextActionManager;
import tn.eluea.kgpt.textactions.TextActionPrompts;
import tn.eluea.kgpt.llm.SimpleAIController;
import tn.eluea.kgpt.listener.GenerativeAIListener;

/**
 * Floating menu activity that appears when text is selected.
 * Shows AI text action options in a floating card.
 */
public class TextActionsMenuActivity extends Activity implements GenerativeAIListener {
    
    public static final String EXTRA_SELECTED_TEXT = "selected_text";
    public static final String EXTRA_READONLY = "readonly";
    
    private static final int ANIMATION_DURATION = 200;
    
    // Languages for translation
    private static final List<String> LANGUAGES = Arrays.asList(
        "Arabic", "English", "French", "Spanish", "German", 
        "Italian", "Russian", "Turkish", "Chinese", "Japanese", 
        "Korean", "Hindi", "Portuguese", "Indonesian"
    );

    private String selectedText;
    private String originalSelectedText; 
    private boolean isReadonly;
    private TextActionManager actionManager;
    private FrameLayout rootLayout;
    private LinearLayout menuCard;
    private LinearLayout cardContentContainer;
    private Handler mainHandler;
    private StringBuilder responseBuilder;
    private ValueAnimator loadingAnimator;
    
    // State
    private TextAction currentAction; 
    private String currentResult;
    private int selectionStart;
    private int selectionEnd;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        if (!SPManager.isReady()) {
            try {
                SPManager.init(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        );
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        );
        
        Intent intent = getIntent();
        selectedText = intent.getStringExtra(EXTRA_SELECTED_TEXT);
        selectionStart = intent.getIntExtra("selection_start", -1);
        selectionEnd = intent.getIntExtra("selection_end", -1);
        originalSelectedText = selectedText;
        isReadonly = intent.getBooleanExtra(EXTRA_READONLY, false);
        
        if (selectedText == null || selectedText.isEmpty()) {
            finish();
            return;
        }
        
        actionManager = new TextActionManager(this);
        actionManager.reloadConfig();
        
        createUI();
    }
    
    private void createUI() {
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setBackgroundColor(Color.parseColor("#80000000"));
        rootLayout.setOnClickListener(v -> animateOutAndFinish());
        
        // Main Card
        menuCard = new LinearLayout(this);
        menuCard.setOrientation(LinearLayout.VERTICAL);
        menuCard.setBackground(createCardBackground());
        menuCard.setElevation(dp(12));
        menuCard.setClickable(true);
        menuCard.setMinimumWidth(dp(200));
        
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        menuParams.gravity = Gravity.CENTER;
        int margin = dp(24);
        menuParams.setMargins(margin, 0, margin, 0);
        menuCard.setLayoutParams(menuParams);
        
        // Content Container
        cardContentContainer = new LinearLayout(this);
        cardContentContainer.setOrientation(LinearLayout.VERTICAL);
        menuCard.addView(cardContentContainer);
        
        rootLayout.addView(menuCard);
        setContentView(rootLayout);
        
        showMainMenu();
        animateIn();
    }
    
    private void showMainMenu() {
        cardContentContainer.removeAllViews();
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        LinearLayout actionsContainer = new LinearLayout(this);
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.CENTER_VERTICAL);
        
        List<TextAction> actions = actionManager.getEnabledActions();
        boolean showLabels = actionManager.shouldShowLabels();
        
        // Standard Actions
        for (int i = 0; i < actions.size(); i++) {
            TextAction action = actions.get(i);
            actionsContainer.addView(createActionButton(action, showLabels));
            actionsContainer.addView(createVerticalDivider());
        }
        
        // Custom Actions
        List<tn.eluea.kgpt.textactions.CustomTextAction> customActions = actionManager.getCustomActions();
        for (tn.eluea.kgpt.textactions.CustomTextAction action : customActions) {
            if (action.enabled) {
                actionsContainer.addView(createActionButton(action, showLabels));
                actionsContainer.addView(createVerticalDivider());
            }
        }
        
        // Remove last divider if exists
        // (Simpler: just add Close button at the end separately)
        
        actionsContainer.addView(createCloseButtonIcon());
        
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.addView(actionsContainer);
        
        container.addView(scrollView);
        cardContentContainer.addView(container);
    }
    
    private void showLanguageSelector() {
        cardContentContainer.removeAllViews();
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setMinimumWidth(dp(250));
        container.setPadding(dp(0), dp(8), dp(0), dp(8));
        
        TextView header = new TextView(this);
        header.setText("Select Language");
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(8), 0, dp(12));
        container.addView(header);
        
        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        
        for (String lang : LANGUAGES) {
            TextView langItem = new TextView(this);
            langItem.setText(lang);
            langItem.setTextColor(Color.parseColor("#DDFFFFFF"));
            langItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            langItem.setPadding(dp(24), dp(12), dp(24), dp(12));
            langItem.setBackgroundResource(getSelectableItemBackground());
            langItem.setClickable(true);
            langItem.setOnClickListener(v -> {
                selectedText = currentResult != null ? currentResult : selectedText;
                processAction(TextAction.TRANSLATE, lang);
            });
            list.addView(langItem);
        }
        
        scrollView.addView(list);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            dp(300) 
        );
        container.addView(scrollView, scrollParams);
        
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        container.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        
        TextView cancelBtn = new TextView(this);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(Color.parseColor("#B3FFFFFF"));
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setPadding(0, dp(12), 0, dp(8));
        cancelBtn.setClickable(true);
        cancelBtn.setBackgroundResource(getSelectableItemBackground());
        cancelBtn.setOnClickListener(v -> {
            if (currentResult != null) {
                showResult(currentResult, currentAction);
            } else {
                showMainMenu();
            }
        });
        container.addView(cancelBtn);

        cardContentContainer.addView(container);
    }
    
    private void showLoading() {
        cardContentContainer.removeAllViews();
        
        LinearLayout loadingView = new LinearLayout(this);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setPadding(dp(24), dp(20), dp(24), dp(20));
        loadingView.setMinimumWidth(dp(200));
        
        TextView generatingText = new TextView(this);
        generatingText.setText("Generating...");
        generatingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        generatingText.setTextColor(Color.WHITE);
        generatingText.setGravity(Gravity.CENTER);
        loadingView.addView(generatingText);
        
        cardContentContainer.addView(loadingView);
        
        generatingText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                generatingText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startWaveAnimation(generatingText);
            }
        });
    }

    private void startWaveAnimation(TextView textView) {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }

        int width = textView.getWidth();
        if (width == 0) return;

        Shader textShader = new LinearGradient(0, 0, width, 0,
                new int[]{Color.GRAY, Color.WHITE, Color.GRAY},
                new float[]{0, 0.5f, 1},
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
    
    private void showResult(String result, TextAction action) {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
            loadingAnimator = null;
        }
        
        currentResult = result;
        cardContentContainer.removeAllViews();
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        ScrollView textScroll = new ScrollView(this);
        textScroll.setFillViewport(true); 
        
        TextView resultText = new TextView(this);
        resultText.setText(result);
        resultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        resultText.setTextColor(Color.WHITE);
        resultText.setLineSpacing(dp(4), 1.0f);
        resultText.setTextIsSelectable(true);
        textScroll.addView(resultText);
        
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int maxHeight = (int) (screenHeight * 0.45);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
           ViewGroup.LayoutParams.MATCH_PARENT,
           ViewGroup.LayoutParams.WRAP_CONTENT
        );
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
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, dp(12), 0, dp(12));
        container.addView(divider, divParams);
        
        // --- Buttons Area ---
        
        // Row 1: Replace | Append
        if (!isReadonly) {
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER);
            
            row1.addView(createButton("Replace", v -> finishWithResult(result)), 
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            
            View s1 = new View(this); 
            row1.addView(s1, new LinearLayout.LayoutParams(dp(8), dp(1)));
            
            row1.addView(createButton("Append", v -> finishWithAppend(result)), 
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            
            container.addView(row1);
            
            // Spacer row
            View sRow = new View(this);
            container.addView(sRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        }
        
        // Row 2: Translate | Copy
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        
        row2.addView(createButton("Translate", v -> showLanguageSelector()), 
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        View s2 = new View(this); 
        row2.addView(s2, new LinearLayout.LayoutParams(dp(8), dp(1)));
        
        row2.addView(createButton("Copy", v -> {
            copyToClipboard(result);
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        container.addView(row2);
        
        // Spacer row
        View sRow2 = new View(this);
        container.addView(sRow2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        // Row 3: Close
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.CENTER);
        row3.addView(createButton("Close", v -> animateOutAndFinish()), 
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            
        container.addView(row3);
        
        cardContentContainer.addView(container);
        
        menuCard.setMinimumWidth(dp(300));
    }
    
    private View createButton(String text, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1AFFFFFF")); 
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), Color.parseColor("#33FFFFFF"));
        btn.setBackground(bg);
        
        btn.setClickable(true);
        btn.setOnClickListener(listener);
        return btn;
    }
    
    private View createActionButton(TextAction action, boolean showLabel) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());
        
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        
        try {
            icon.setImageResource(action.iconRes);
            icon.setColorFilter(Color.parseColor(action.color));
        } catch (Exception e) {
            GradientDrawable fallback = new GradientDrawable();
            fallback.setShape(GradientDrawable.OVAL);
            fallback.setColor(Color.parseColor(action.color));
            icon.setBackground(fallback);
        }
        button.addView(icon);
        
        if (showLabel) {
            TextView label = new TextView(this);
            label.setText(action.labelEn);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTextColor(Color.parseColor("#B3FFFFFF"));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.topMargin = dp(4);
            label.setLayoutParams(labelParams);
            button.addView(label);
        }
        
        button.setOnClickListener(v -> processAction(action, null));
        return button;
    }

    private View createActionButton(tn.eluea.kgpt.textactions.CustomTextAction action, boolean showLabel) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());
        
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        
        // Unified Icon: Magic Star Filled
        icon.setImageResource(R.drawable.ic_star_filled);
        icon.setColorFilter(Color.parseColor("#FFC107")); // Amber

        button.addView(icon);
        
        if (showLabel) {
            TextView label = new TextView(this);
            label.setText(action.name);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTextColor(Color.parseColor("#B3FFFFFF"));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.topMargin = dp(4);
            label.setLayoutParams(labelParams);
            button.addView(label);
        }
        
        button.setOnClickListener(v -> processAction(action));
        return button;
    }
    
    private View createCloseButtonIcon() {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setClickable(true);
        button.setBackgroundResource(getSelectableItemBackground());

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        icon.setColorFilter(Color.parseColor("#80FFFFFF"));
        button.addView(icon);
        
        button.setOnClickListener(v -> animateOutAndFinish());
        return button;
    }

    private View createVerticalDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(28));
        params.setMargins(dp(4), 0, dp(4), 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        return divider;
    }
    
    private GradientDrawable createCardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor("#E6212121"));
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), Color.parseColor("#33FFFFFF"));
        return background;
    }
    
    private int getSelectableItemBackground() {
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        return outValue.resourceId;
    }

    private void processAction(TextAction action, String targetInfo) {
        currentAction = action;
        showLoading();
        
        responseBuilder = new StringBuilder();
        
        String systemMessage;
        if (action == TextAction.TRANSLATE && targetInfo != null) {
            systemMessage = TextActionPrompts.getSystemMessage(action, targetInfo);
        } else {
            systemMessage = actionManager.getActionPrompt(action);
        }
        String prompt = TextActionPrompts.buildPrompt(action, selectedText);
        
        SimpleAIController aiController = new SimpleAIController();
        aiController.addListener(this);
        
        new Thread(() -> {
            aiController.generateResponse(prompt, systemMessage);
        }).start();
    }

    private void processAction(tn.eluea.kgpt.textactions.CustomTextAction action) {
        // currentAction is TextAction enum, so it will be null for custom action
        // We need to handle this in showResult or change logic
        currentAction = null; 
        showLoading();
        
        responseBuilder = new StringBuilder();
        
        String systemMessage = action.prompt;
        // For custom actions, we just send standard text + system prompt
        // We can reuse buildPrompt(null, text) which basically just returns text or wrapped text
        // Or we can construct it manually. TextActionPrompts.buildPrompt might expect an enum.
        
        // Let's rely on SimpleAIController handling system message properly.
        // We just send the text as user prompt.
        String prompt = "Text: \"" + selectedText + "\"";
        
        SimpleAIController aiController = new SimpleAIController();
        aiController.addListener(this);
        
        new Thread(() -> {
            aiController.generateResponse(prompt, systemMessage);
        }).start();
    }
    
    // Broadcast method with selection args
    private void finishWithResult(String text) {
        Intent intent = new Intent(SelectionHandler.ACTION_COMMIT_TEXT);
        intent.putExtra(SelectionHandler.EXTRA_TEXT_TO_COMMIT, text);
        intent.putExtra("selection_start", selectionStart);
        intent.putExtra("selection_end", selectionEnd);
        sendBroadcast(intent);
        
        finish();
    }
    
    private void finishWithAppend(String text) {
        String finalText = originalSelectedText + " " + text;
        finishWithResult(finalText);
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AI Result", text);
            clipboard.setPrimaryClip(clip);
        }
    }
    
    @Override
    public void onAIPrepare() { }
    
    @Override
    public void onAINext(String chunk) {
        responseBuilder.append(chunk);
    }
    
    @Override
    public void onAIError(Throwable t) {
        mainHandler.post(() -> {
            Toast.makeText(this, "AI Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            if (currentResult != null) {
                showResult(currentResult, currentAction);
            } else {
                showMainMenu();
            }
        });
    }
    
    @Override
    public void onAIComplete() {
        String result = responseBuilder.toString();
        mainHandler.post(() -> showResult(result, currentAction));
    }
    
    private void animateIn() {
        if (menuCard == null) return;
        menuCard.setAlpha(0f);
        menuCard.setScaleX(0.8f);
        menuCard.setScaleY(0.8f);
        menuCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }
    
    private void animateOutAndFinish() {
        if (menuCard == null) {
            finish();
            return;
        }
        menuCard.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(ANIMATION_DURATION / 2)
            .withEndAction(this::finish)
            .start();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            animateOutAndFinish();
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public void onBackPressed() {
        if (cardContentContainer.getChildCount() > 0) {
            animateOutAndFinish();
        } else {
            animateOutAndFinish();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            getResources().getDisplayMetrics()
        );
    }
}
