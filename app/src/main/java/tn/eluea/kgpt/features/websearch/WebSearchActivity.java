/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.features.websearch;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.UiInteractor;

/**
 * Web search activity that displays search results in a floating bottom sheet.
 * 
 * Receives search engine preference via Intent from Xposed Module,
 * avoiding the need to initialize SPManager in KGPT's context.
 */
public class WebSearchActivity extends AppCompatActivity {

    private static final String TAG = "WebSearchActivity";
    private static final int MIN_SHEET_HEIGHT_DP = 200;
    private static final int FULLSCREEN_THRESHOLD_DP = 100;

    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvUrl;
    private EditText etUrl;
    private LinearLayout urlDisplayContainer;
    private LinearLayout urlEditContainer;
    private View bottomSheetContainer;
    private View handleBar;
    private View dimBackground;
    private View fullscreenTopSpacer;
    private String currentUrl;
    private String searchEngine;

    private float initialTouchY;
    private int initialSheetMargin;
    private boolean isFullscreen = false;
    private int screenHeight;
    private int statusBarHeight = 0;
    private int navBarHeight = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate started");

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_web_search);

        // Apply Blur if enabled (optimized for performance)
        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_gpt_ui",
                android.content.Context.MODE_PRIVATE);
        boolean isBlurEnabled = prefs.getBoolean("blur_enabled", true);
        int blurIntensity = prefs.getInt("blur_intensity", 25);
        int blurTintColor = prefs.getInt("blur_tint_color", 0); // 0 is Color.TRANSPARENT

        // Check if device is low-end to reduce blur intensity
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isLowRamDevice = am != null && am.isLowRamDevice();
        int maxBlurRadius = isLowRamDevice ? 15 : 30;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            if (isBlurEnabled && blurIntensity > 0) {
                int blurRadius = Math.min((blurIntensity * maxBlurRadius) / 100, maxBlurRadius);
                params.setBlurBehindRadius(blurRadius);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            }
            getWindow().setAttributes(params);
        }

        // Fix status bar icons visibility (force white icons for dark background)
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        // Edge-to-Edge: Transparent System Bars
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        screenHeight = getResources().getDisplayMetrics().heightPixels;

        currentUrl = getIntent().getStringExtra(UiInteractor.EXTRA_WEBVIEW_URL);
        String title = getIntent().getStringExtra(UiInteractor.EXTRA_WEBVIEW_TITLE);
        // Get search engine from Intent (sent by Xposed Module)
        searchEngine = getIntent().getStringExtra(UiInteractor.EXTRA_SEARCH_ENGINE);
        if (searchEngine == null) {
            searchEngine = "duckduckgo";
        }

        Log.d(TAG, "Original URL: " + currentUrl + ", Title: " + title + ", SearchEngine: " + searchEngine);

        if (currentUrl == null) {
            Log.e(TAG, "URL is null, finishing");
            finish();
            return;
        }

        // Apply search engine from Intent
        currentUrl = applySearchEngine(currentUrl);
        Log.d(TAG, "Final URL after applying search engine: " + currentUrl);

        initViews();

        // Apply Blur Tint to Dim Background
        if (dimBackground != null) {
            if (blurTintColor != 0) { // Not Transparent
                int alpha = 120;
                int colorWithAlpha = android.graphics.Color.argb(alpha,
                        android.graphics.Color.red(blurTintColor),
                        android.graphics.Color.green(blurTintColor),
                        android.graphics.Color.blue(blurTintColor));
                dimBackground.setBackgroundColor(colorWithAlpha);
            } else {
                // Default Dim
                dimBackground.setBackgroundColor(0x80000000);
            }
        }

        setupDragBehavior();
        setupUrlEditing();
        configureWebView();

        tvUrl.setText(currentUrl);

        webView.loadUrl(currentUrl);
        hideKeyboard();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (urlEditContainer != null && urlEditContainer.getVisibility() == View.VISIBLE) {
                    hideUrlEdit();
                } else if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    /**
     * Apply the user's preferred search engine to the URL
     * If the URL is a DuckDuckGo search URL, convert it to the preferred engine
     */
    private String applySearchEngine(String url) {
        if (url == null)
            return url;

        // Extract query from known search engine URLs
        String query = extractSearchQuery(url);
        if (query != null && !query.isEmpty()) {
            // Build URL using search engine from Intent
            Log.d(TAG, "Applying search engine: " + searchEngine + " for query: " + query);
            return buildSearchUrl(searchEngine, query);
        }

        return url;
    }

    /**
     * Build search URL for the given engine and query
     */
    private String buildSearchUrl(String engine, String query) {
        String encodedQuery;
        try {
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            encodedQuery = query;
        }

        switch (engine) {
            case "google":
                return "https://www.google.com/search?q=" + encodedQuery;
            case "bing":
                return "https://www.bing.com/search?q=" + encodedQuery;
            case "yahoo":
                return "https://search.yahoo.com/search?p=" + encodedQuery;
            case "yandex":
                return "https://yandex.com/search/?text=" + encodedQuery;
            case "brave":
                return "https://search.brave.com/search?q=" + encodedQuery;
            case "ecosia":
                return "https://www.ecosia.org/search?q=" + encodedQuery;
            case "qwant":
                return "https://www.qwant.com/?q=" + encodedQuery;
            case "startpage":
                return "https://www.startpage.com/do/dsearch?query=" + encodedQuery;
            case "perplexity":
                return "https://www.perplexity.ai/?q=" + encodedQuery;
            case "phind":
                return "https://www.phind.com/search?q=" + encodedQuery;
            case "duckduckgo":
            default:
                return "https://duckduckgo.com/?q=" + encodedQuery;
        }
    }

    /**
     * Extract search query from various search engine URLs
     */
    private String extractSearchQuery(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null)
                return null;

            // DuckDuckGo
            if (host.contains("duckduckgo.com")) {
                return uri.getQueryParameter("q");
            }
            // Google
            if (host.contains("google.com")) {
                return uri.getQueryParameter("q");
            }
            // Bing
            if (host.contains("bing.com")) {
                return uri.getQueryParameter("q");
            }
            // Yahoo
            if (host.contains("yahoo.com")) {
                return uri.getQueryParameter("p");
            }
            // Yandex
            if (host.contains("yandex.com")) {
                return uri.getQueryParameter("text");
            }
            // Brave
            if (host.contains("brave.com")) {
                return uri.getQueryParameter("q");
            }
            // Ecosia
            if (host.contains("ecosia.org")) {
                return uri.getQueryParameter("q");
            }
            // Qwant
            if (host.contains("qwant.com")) {
                return uri.getQueryParameter("q");
            }
            // StartPage
            if (host.contains("startpage.com")) {
                return uri.getQueryParameter("query");
            }
            // Perplexity
            if (host.contains("perplexity.ai")) {
                return uri.getQueryParameter("q");
            }
            // Phind
            if (host.contains("phind.com")) {
                return uri.getQueryParameter("q");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting search query", e);
        }
        return null;
    }

    private void initViews() {
        tvUrl = findViewById(R.id.tv_url);
        etUrl = findViewById(R.id.et_url);
        urlDisplayContainer = findViewById(R.id.url_display_container);
        urlEditContainer = findViewById(R.id.url_edit_container);
        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnOpenBrowser = findViewById(R.id.btn_open_browser);
        progressBar = findViewById(R.id.progress_bar);
        webView = findViewById(R.id.web_view);
        dimBackground = findViewById(R.id.dim_background);
        bottomSheetContainer = findViewById(R.id.bottom_sheet_container);
        handleBar = findViewById(R.id.handle_bar);
        fullscreenTopSpacer = findViewById(R.id.fullscreen_top_spacer);

        // Hide handle bar and spacer as requested
        if (handleBar != null)
            handleBar.setVisibility(View.GONE);
        if (fullscreenTopSpacer != null)
            fullscreenTopSpacer.setVisibility(View.GONE);

        btnClose.setOnClickListener(v -> finish());

        btnOpenBrowser.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(browserIntent);
            finish();
        });

        dimBackground.setOnClickListener(v -> finish());

        bottomSheetContainer.setOnClickListener(v -> {
            // Consume click
        });

        // Handle edge-to-edge insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomSheetContainer, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarHeight = insets.top;
            navBarHeight = insets.bottom;
            applyPadding();
            return windowInsets;
        });
    }

    private void applyPadding() {
        if (bottomSheetContainer == null)
            return;

        // Removed extra padding completely for minimal clearance as requested
        int topPadding = isFullscreen ? statusBarHeight : 0;
        bottomSheetContainer.setPadding(bottomSheetContainer.getPaddingLeft(),
                topPadding,
                bottomSheetContainer.getPaddingRight(),
                navBarHeight);
    }

    private void setupUrlEditing() {
        // Note: Opening edit mode is handled by Drag/Touch listener on
        // urlDisplayContainer

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                String newUrl = etUrl.getText().toString().trim();
                if (!newUrl.isEmpty()) {
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        newUrl = "https://" + newUrl;
                    }
                    currentUrl = newUrl;
                    tvUrl.setText(currentUrl);
                    webView.loadUrl(currentUrl);
                }
                hideUrlEdit();
                return true;
            }
            return false;
        });

        ImageView btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnCancelEdit.setOnClickListener(v -> hideUrlEdit());

        ImageView btnGoUrl = findViewById(R.id.btn_go_url);
        btnGoUrl.setOnClickListener(v -> {
            String newUrl = etUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "https://" + newUrl;
                }
                currentUrl = newUrl;
                tvUrl.setText(currentUrl);
                webView.loadUrl(currentUrl);
            }
            hideUrlEdit();
        });
    }

    private void hideUrlEdit() {
        urlEditContainer.setVisibility(View.GONE);
        urlDisplayContainer.setVisibility(View.VISIBLE);
        hideKeyboard();
    }

    private void showKeyboard() {
        if (etUrl != null) {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), etUrl);
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.ime());
            }
        }
    }

    private void setupDragBehavior() {
        // We attach the drag listener to the URL display container directly.
        // We implement simple logic to distinguish Click (edit) vs Drag (move).

        urlDisplayContainer.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private float startRawY;
            private int startMargin;
            private boolean isDragging = false;
            private static final int TOUCH_SLOP = 20; // px
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRawY = event.getRawY();
                        // Also track relative Y if needed, but RawY is better for screen drag
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomSheetContainer
                                .getLayoutParams();
                        startMargin = params.topMargin;
                        isDragging = false;
                        startTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startRawY;

                        if (!isDragging && Math.abs(deltaY) > TOUCH_SLOP) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            int newMargin = (int) (startMargin + deltaY);
                            int minMargin = 0;
                            int maxMargin = screenHeight - dpToPx(MIN_SHEET_HEIGHT_DP);
                            newMargin = Math.max(minMargin, Math.min(maxMargin, newMargin));

                            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer
                                    .getLayoutParams();
                            layoutParams.topMargin = newMargin;
                            bottomSheetContainer.setLayoutParams(layoutParams);

                            updateFullscreenState(newMargin);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startTime;
                        if (!isDragging && duration < 500) {
                            // It was a tap! Trigger edit mode.
                            startUrlEdit();
                        } else {
                            // Finish drag
                            ViewGroup.MarginLayoutParams finalParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer
                                    .getLayoutParams();
                            int currentMargin = finalParams.topMargin;

                            if (currentMargin < dpToPx(FULLSCREEN_THRESHOLD_DP)) {
                                animateToFullscreen();
                            } else if (currentMargin > screenHeight - dpToPx(MIN_SHEET_HEIGHT_DP + 50)) {
                                finish();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return false;
            }
        });

        // Remove the standard click listener since we handle it in onTouch
        urlDisplayContainer.setOnClickListener(null);
    }

    private void startUrlEdit() {
        urlDisplayContainer.setVisibility(View.GONE);
        urlEditContainer.setVisibility(View.VISIBLE);
        etUrl.setText(currentUrl);
        etUrl.requestFocus();
        etUrl.setSelection(etUrl.getText().length());
        showKeyboard();
    }

    private void updateFullscreenState(int topMargin) {
        boolean shouldBeFullscreen = topMargin < dpToPx(FULLSCREEN_THRESHOLD_DP);

        if (shouldBeFullscreen != isFullscreen) {
            isFullscreen = shouldBeFullscreen;

            // We no longer toggle handleBar or spacer visibility here
            // as they are permanently hidden.

            dimBackground.setAlpha(isFullscreen ? 0f : 1f);

            // Keep the same background style, just remove rounded corners in fullscreen
            if (isFullscreen) {
                bottomSheetContainer.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.surface_color));
                applyPadding();
            } else {
                bottomSheetContainer.setBackgroundResource(R.drawable.bg_bottom_sheet);
                applyPadding();
            }
        }
    }

    private void animateToFullscreen() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomSheetContainer.getLayoutParams();
        int startMargin = params.topMargin;

        ValueAnimator animator = ValueAnimator.ofInt(startMargin, 0);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer
                    .getLayoutParams();
            layoutParams.topMargin = value;
            bottomSheetContainer.setLayoutParams(layoutParams);
            updateFullscreenState(value);
        });
        animator.start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        Log.d(TAG, "Configuring WebView");

        // Use hardware acceleration only on capable devices
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isLowRamDevice = am != null && am.isLowRamDevice();

        if (isLowRamDevice) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Use cache more aggressively on low-end devices
        if (isLowRamDevice) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }

        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_color));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);
                progressBar.setVisibility(View.GONE);
                tvUrl.setText(url);
                currentUrl = url;
                webView.requestLayout();
                webView.invalidate();
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "Page started loading: " + url);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for " + failingUrl);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "Loading progress: " + newProgress + "%");
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (webView != null) {
            webView.onResume();
        }
        hideKeyboard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // Properly cleanup WebView to prevent crashes on re-open
        if (webView != null) {
            // Stop loading
            webView.stopLoading();

            // Clear WebView
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");

            // Remove from parent before destroying
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }

            // Destroy WebView
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
    }

    // onBackPressed removed. Handled by OnBackPressedDispatcher in onCreate.
}
