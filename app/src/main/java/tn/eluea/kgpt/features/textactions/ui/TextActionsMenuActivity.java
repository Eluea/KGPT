/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.features.textactions.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.features.textactions.presentation.TextActionsUiComposer;
import tn.eluea.kgpt.features.textactions.SelectionHandler;
import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.data.TextActionManager;
import tn.eluea.kgpt.features.textactions.TextActionPrompts;
import tn.eluea.kgpt.llm.SimpleAIController;
import tn.eluea.kgpt.listener.GenerativeAIListener;

import tn.eluea.kgpt.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Floating menu activity that appears when text is selected.
 * Shows AI text action options in a floating card.
 */
public class TextActionsMenuActivity extends AppCompatActivity
        implements GenerativeAIListener, TextActionsUiComposer.UiActionListener {

    public static final String EXTRA_SELECTED_TEXT = "selected_text";
    public static final String EXTRA_READONLY = "readonly";

    // Languages for translation
    // private static final List<String> LANGUAGES = Arrays.asList(...);

    private String selectedText;
    private String originalSelectedText;
    private boolean isReadonly;
    private boolean fromProcessText = false; // Flag to determine return method

    private TextActionManager actionManager;
    private TextActionsUiComposer uiComposer;
    private Handler mainHandler;
    private StringBuilder responseBuilder;

    // State
    private TextAction currentAction;
    private String currentResult;
    private int selectionStart;
    private int selectionEnd;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(tn.eluea.kgpt.util.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainHandler = new Handler(Looper.getMainLooper());

        if (!SPManager.isReady()) {
            try {
                SPManager.init(getApplicationContext());
                UiInteractor.init(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Ensure UiInteractor is also ready if SPManager was ready but UiInteractor
            // wasn't
            UiInteractor.init(getApplicationContext());
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        // Apply blur behind the floating window
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyBlurToWindow(getWindow(), this);

        Intent intent = getIntent();
        selectedText = intent.getStringExtra(EXTRA_SELECTED_TEXT);
        selectionStart = intent.getIntExtra("selection_start", -1);
        selectionEnd = intent.getIntExtra("selection_end", -1);
        fromProcessText = intent.getBooleanExtra("from_process_text", false);
        originalSelectedText = selectedText;
        isReadonly = intent.getBooleanExtra(EXTRA_READONLY, false);

        if (selectedText == null || selectedText.isEmpty()) {
            finish();
            return;
        }

        actionManager = tn.eluea.kgpt.core.di.ServiceLocator.getInstance().createTextActionManager(this);
        actionManager.reloadConfig();

        uiComposer = new TextActionsUiComposer(this, this);
        uiComposer.createUI(this);

        showMainMenu();
        uiComposer.animateIn();

        if (this instanceof androidx.activity.ComponentActivity) {
            ((androidx.activity.ComponentActivity) this).getOnBackPressedDispatcher().addCallback(this,
                    new androidx.activity.OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            uiComposer.animateOut(() -> finish());
                        }
                    });
        }
    }

    // --- UI Action Listener Implementation ---

    @Override
    public void onActionClicked(TextAction action) {
        if (action == TextAction.DOWNLOAD) {
            if (!tn.eluea.kgpt.features.downloader.core.DownloaderEngine.getInstance().isCoreInstalled(this)) {
                new tn.eluea.kgpt.features.downloader.ui.CoreUpdateBottomSheet(this).show();
                return;
            }
            java.util.List<String> urls = tn.eluea.kgpt.features.downloader.core.MediaUtils.extractAllUrls(selectedText);
            if (urls.isEmpty()) {
                if (selectedText != null && !selectedText.trim().isEmpty() && (selectedText.startsWith("http://") || selectedText.startsWith("https://"))) {
                    urls.add(selectedText.trim());
                }
            }
            if (urls.isEmpty()) {
                if (selectedText != null && !selectedText.trim().isEmpty()) {
                    // Smart Search: Selected text contains no URLs, treat as YouTube search query
                    new tn.eluea.kgpt.features.downloader.ui.YouTubeSearchBottomSheet(this, selectedText.trim()).show();
                    uiComposer.animateOut(this::finish);
                    return;
                }
                Toast.makeText(this, R.string.toast_invalid_url, Toast.LENGTH_SHORT).show();
                return;
            }
            if (urls.size() == 1) {
                UiInteractor.getInstance().showMediaDownloaderDialog(urls.get(0));
                uiComposer.animateOut(this::finish);
            } else {
                tn.eluea.kgpt.features.downloader.ui.LinkSelectionBottomSheet sheet =
                        new tn.eluea.kgpt.features.downloader.ui.LinkSelectionBottomSheet(this, urls, (tn.eluea.kgpt.features.downloader.ui.LinkSelectionBottomSheet.OnLinkSelectedListener) selectedUrl -> {
                            UiInteractor.getInstance().showMediaDownloaderDialog(selectedUrl);
                            uiComposer.animateOut(this::finish);
                        });
                sheet.setOnDismissListener(d -> uiComposer.animateOut(this::finish));
                sheet.show();
            }
            return;
        }
        processAction(action, null);
    }

    @Override
    public void onCustomActionClicked(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action) {
        processCustomAction(action);
    }

    @Override
    public void onCloseClicked() {
        uiComposer.animateOut(this::finish);
    }

    @Override
    public void onBackgroundClicked() {
        uiComposer.animateOut(this::finish);
    }

    @Override
    public void onTranslateClicked() {
        selectedText = currentResult != null ? currentResult : selectedText;
        String[] languagesArray = getResources().getStringArray(R.array.languages_list);
        uiComposer.showLanguageSelector(Arrays.asList(languagesArray));
    }

    @Override
    public void onLanguageSelected(String language) {
        processAction(TextAction.TRANSLATE, language);
    }

    @Override
    public void onCancelLanguageSelection() {
        if (currentResult != null) {
            uiComposer.showResult(currentResult, isReadonly);
        } else {
            showMainMenu();
        }
    }

    @Override
    public void onReplaceClicked() {
        if (currentResult != null)
            finishWithResult(currentResult);
    }

    @Override
    public void onAppendClicked() {
        if (currentResult != null) {
            // Append result after the original selected text
            // We need to replace the selection with: original + space + result
            String finalText = originalSelectedText + " " + currentResult;
            finishWithResult(finalText);
        }
    }

    @Override
    public void onCopyClicked() {
        if (currentResult != null) {
            copyToClipboard(currentResult);
            Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
        }
    }

    // --- Logic ---

    private void showMainMenu() {
        List<TextAction> actions = new java.util.ArrayList<>(actionManager.getEnabledActions());

        // Check if selected text contains media links
        boolean hasMediaLink = false;
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            java.util.List<String> detectedUrls = tn.eluea.kgpt.features.downloader.core.MediaUtils.extractAllUrls(selectedText);
            hasMediaLink = !detectedUrls.isEmpty() || selectedText.startsWith("http://") || selectedText.startsWith("https://");
        }

        if (hasMediaLink) {
            // Promote DOWNLOAD to the very first position so user sees it immediately
            actions.remove(TextAction.DOWNLOAD);
            actions.add(0, TextAction.DOWNLOAD);
        } else if (!actions.contains(TextAction.DOWNLOAD)) {
            actions.add(0, TextAction.DOWNLOAD);
        }

        uiComposer.showMainMenu(
                actions,
                actionManager.getCustomActions(),
                actionManager.shouldShowLabels());
    }

    private void processAction(TextAction action, String targetInfo) {
        currentAction = action;
        uiComposer.showLoading();

        responseBuilder = new StringBuilder();

        String systemMessage;
        if (action == TextAction.TRANSLATE && targetInfo != null) {
            systemMessage = TextActionPrompts.getSystemMessage(action, targetInfo, this);
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

    private void processCustomAction(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action) {
        currentAction = null;
        uiComposer.showLoading();

        responseBuilder = new StringBuilder();

        String systemMessage = action.prompt;
        // For custom actions, we just send the text as user prompt.
        String prompt = "Text: \"" + selectedText + "\"";

        SimpleAIController aiController = new SimpleAIController();
        aiController.addListener(this);

        new Thread(() -> {
            aiController.generateResponse(prompt, systemMessage);
        }).start();
    }

    /**
     * Replace the original selected text with the new text.
     * 
     * Two paths:
     * 1. From ProcessTextActivity (PROCESS_TEXT intent): Return result via
     * setResult()
     * 2. From SelectionHandler (keyboard): Send broadcast to commit text
     */
    private void finishWithResult(String text) {
        if (fromProcessText) {
            // Return result to ProcessTextActivity which will forward to calling app
            Intent resultIntent = new Intent();
            resultIntent.putExtra(ProcessTextActivity.EXTRA_RESULT_TEXT, text);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Send broadcast to SelectionHandler to commit text
            Intent intent = new Intent(SelectionHandler.ACTION_COMMIT_TEXT);
            intent.putExtra(SelectionHandler.EXTRA_TEXT_TO_COMMIT, text);
            intent.putExtra("selection_start", selectionStart);
            intent.putExtra("selection_end", selectionEnd);
            sendBroadcast(intent);
            finish();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AI Result", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    // --- AI Listener ---

    @Override
    public void onAIPrepare() {
    }

    @Override
    public void onAINext(String chunk) {
        responseBuilder.append(chunk);
    }

    @Override
    public void onAIError(Throwable t) {
        mainHandler.post(() -> {
            Toast.makeText(this, getString(R.string.msg_ai_error, t.getMessage()), Toast.LENGTH_LONG).show();
            if (currentResult != null) {
                uiComposer.showResult(currentResult, isReadonly);
            } else {
                showMainMenu();
            }
        });
    }

    @Override
    public void onAIComplete() {
        currentResult = responseBuilder.toString();
        mainHandler.post(() -> uiComposer.showResult(currentResult, isReadonly));
    }

    // --- Lifecycle & System ---

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            uiComposer.animateOut(this::finish);
            return true;
        }
        return super.onTouchEvent(event);
    }

    // onBackPressed removed. Handled by OnBackPressedDispatcher in onCreate.

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiComposer.cancelLoading();
    }

    public int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics());
    }
}
