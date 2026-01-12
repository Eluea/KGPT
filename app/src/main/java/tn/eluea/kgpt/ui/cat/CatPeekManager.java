/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.ui.cat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.view.SparkleView;

/**
 * Manages the cat peek easter egg feature.
 * The cat appears every few minutes and when caught, shows a bottom sheet
 * encouraging users to star the GitHub repo.
 */
public class CatPeekManager {

    private static final String GITHUB_REPO_URL = "https://github.com/Eluea/KGPT/";
    private static final long PEEK_INTERVAL_MS = 8 * 1000; // 8 seconds (for testing, change to 3 * 60 * 1000 for production)
    private static final long PEEK_DURATION_MS = 3000; // 3 seconds visible
    private static final long ANIMATION_DURATION_MS = 400;

    private final Context context;
    private final Handler handler;
    private View catView;
    private boolean isRunning = false;
    private boolean isCatVisible = false;

    private final Runnable peekRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && catView != null) {
                showCat();
                handler.postDelayed(this, PEEK_INTERVAL_MS);
            }
        }
    };

    public CatPeekManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize with the cat container View
     */
    public void init(View catView) {
        this.catView = catView;
        if (catView != null) {
            catView.setVisibility(View.GONE);
            catView.setOnClickListener(v -> onCatCaught());
        }
    }

    /**
     * Start the cat peek timer
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            // First appearance after the interval
            handler.postDelayed(peekRunnable, PEEK_INTERVAL_MS);
        }
    }

    /**
     * Stop the cat peek timer
     */
    public void stop() {
        isRunning = false;
        handler.removeCallbacks(peekRunnable);
        if (catView != null) {
            catView.setVisibility(View.GONE);
        }
    }

    /**
     * Show the cat with animation (slide up from behind card)
     */
    private void showCat() {
        if (catView == null || isCatVisible) return;

        isCatVisible = true;
        
        // Cat starts hidden behind card (positive Y = down), animates up (negative Y)
        float peekDistance = catView.getContext().getResources().getDisplayMetrics().density * 12f;
        
        catView.setVisibility(View.VISIBLE);
        catView.setAlpha(1f);
        catView.setTranslationY(peekDistance); // Start behind card edge
        catView.setTranslationZ(0f); // Start behind card visually

        // Animate up to peek above card
        ObjectAnimator animIn = ObjectAnimator.ofFloat(catView, "translationY", peekDistance, -peekDistance);
        animIn.setDuration(ANIMATION_DURATION_MS);
        animIn.setInterpolator(new OvershootInterpolator(1.2f));
        animIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Raise Z only after animation completes (cat is now above card edge)
                if (catView != null) {
                    catView.setTranslationZ(20f);
                }
            }
        });
        animIn.start();

        // Auto-hide after duration
        handler.postDelayed(this::hideCat, PEEK_DURATION_MS);
    }

    /**
     * Hide the cat with animation (slide down behind card)
     */
    private void hideCat() {
        if (catView == null || !isCatVisible) return;

        float peekDistance = catView.getContext().getResources().getDisplayMetrics().density * 12f;

        // Lower Z before animation so cat goes behind card
        catView.setTranslationZ(0f);

        // Animate down to hide behind card
        ObjectAnimator animOut = ObjectAnimator.ofFloat(catView, "translationY", -peekDistance, peekDistance);
        animOut.setDuration(ANIMATION_DURATION_MS / 2);
        animOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (catView != null) {
                    catView.setVisibility(View.GONE);
                }
                isCatVisible = false;
            }
        });
        animOut.start();
    }

    /**
     * Called when user catches the cat
     */
    private void onCatCaught() {
        Log.d("CatPeek", "Cat caught! Showing bottom sheet...");
        
        // Hide cat immediately
        handler.removeCallbacksAndMessages(null);
        if (catView != null) {
            catView.setVisibility(View.GONE);
        }
        isCatVisible = false;

        // Show bottom sheet
        try {
            showCaughtBottomSheet();
        } catch (Exception e) {
            Log.e("CatPeek", "Error showing bottom sheet", e);
            Toast.makeText(context, "Meow! You caught me! 🐱", Toast.LENGTH_SHORT).show();
        }

        // Restart timer
        if (isRunning) {
            handler.postDelayed(peekRunnable, PEEK_INTERVAL_MS);
        }
    }

    /**
     * Get Activity from Context
     */
    private Activity getActivity() {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    /**
     * Show the "caught" bottom sheet
     */
    private void showCaughtBottomSheet() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            Log.e("CatPeek", "Activity is null or finishing");
            return;
        }
        
        Log.d("CatPeek", "Creating bottom sheet...");
        
        View sheetView = LayoutInflater.from(activity)
                .inflate(R.layout.bottom_sheet_cat_caught, null);
        
        BottomSheetHelper.applyTheme(activity, sheetView);
        
        FloatingBottomSheet dialog = new FloatingBottomSheet(activity);
        dialog.setContentView(sheetView);

        MaterialButton btnStar = sheetView.findViewById(R.id.btn_star_github);
        TextView tvMaybeLater = sheetView.findViewById(R.id.tv_maybe_later);
        SparkleView sparkleView = sheetView.findViewById(R.id.sparkle_view);

        // Start sparkle animation
        if (sparkleView != null) {
            sparkleView.post(() -> sparkleView.startAnimation());
        }

        btnStar.setOnClickListener(v -> {
            if (sparkleView != null) {
                sparkleView.stopAnimation();
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL));
            activity.startActivity(intent);
            dialog.dismiss();
        });

        tvMaybeLater.setOnClickListener(v -> {
            if (sparkleView != null) {
                sparkleView.stopAnimation();
            }
            dialog.dismiss();
        });

        Log.d("CatPeek", "Showing dialog...");
        dialog.show();
    }

    /**
     * Force show the cat (for testing)
     */
    public void forceShow() {
        showCat();
    }
}
