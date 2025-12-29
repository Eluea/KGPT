package tn.eluea.kgpt.external;

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate started");
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN 
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_web_search);
        
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
        setupDragBehavior();
        setupUrlEditing();
        configureWebView();
        
        tvUrl.setText(currentUrl);
        
        webView.loadUrl(currentUrl);
        hideKeyboard();
    }
    
    /**
     * Apply the user's preferred search engine to the URL
     * If the URL is a DuckDuckGo search URL, convert it to the preferred engine
     */
    private String applySearchEngine(String url) {
        if (url == null) return url;
        
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
            if (host == null) return null;
            
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
    }
    
    private void setupUrlEditing() {
        urlDisplayContainer.setOnClickListener(v -> {
            urlDisplayContainer.setVisibility(View.GONE);
            urlEditContainer.setVisibility(View.VISIBLE);
            etUrl.setText(currentUrl);
            etUrl.requestFocus();
            etUrl.setSelection(etUrl.getText().length());
            showKeyboard();
        });
        
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
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), etUrl);
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.ime());
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupDragBehavior() {
        View dragHandle = findViewById(R.id.drag_handle_area);
        
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomSheetContainer.getLayoutParams();
                    initialSheetMargin = params.topMargin;
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - initialTouchY;
                    int newMargin = (int) (initialSheetMargin + deltaY);
                    
                    int minMargin = 0;
                    int maxMargin = screenHeight - dpToPx(MIN_SHEET_HEIGHT_DP);
                    newMargin = Math.max(minMargin, Math.min(maxMargin, newMargin));
                    
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer.getLayoutParams();
                    layoutParams.topMargin = newMargin;
                    bottomSheetContainer.setLayoutParams(layoutParams);
                    
                    updateFullscreenState(newMargin);
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ViewGroup.MarginLayoutParams finalParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer.getLayoutParams();
                    int currentMargin = finalParams.topMargin;
                    
                    if (currentMargin < dpToPx(FULLSCREEN_THRESHOLD_DP)) {
                        animateToFullscreen();
                    } else if (currentMargin > screenHeight - dpToPx(MIN_SHEET_HEIGHT_DP + 50)) {
                        finish();
                    }
                    return true;
            }
            return false;
        });
    }
    
    private void updateFullscreenState(int topMargin) {
        boolean shouldBeFullscreen = topMargin < dpToPx(FULLSCREEN_THRESHOLD_DP);
        
        if (shouldBeFullscreen != isFullscreen) {
            isFullscreen = shouldBeFullscreen;
            
            handleBar.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
            fullscreenTopSpacer.setVisibility(isFullscreen ? View.VISIBLE : View.GONE);
            dimBackground.setAlpha(isFullscreen ? 0f : 1f);
            
            // Keep the same background style, just remove rounded corners in fullscreen
            if (isFullscreen) {
                bottomSheetContainer.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.surface_color)
                );
            } else {
                bottomSheetContainer.setBackgroundResource(R.drawable.bg_bottom_sheet);
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
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomSheetContainer.getLayoutParams();
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
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        Log.d(TAG, "Configuring WebView");
        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
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
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
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
        if (webView != null) {
            webView.destroy();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (urlEditContainer.getVisibility() == View.VISIBLE) {
            hideUrlEdit();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
