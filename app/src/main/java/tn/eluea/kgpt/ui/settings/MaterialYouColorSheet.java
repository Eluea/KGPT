package tn.eluea.kgpt.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

/**
 * A dedicated floating bottom sheet for Material You color picker.
 * Decoupled from FloatingBottomSheet to avoid implicit blur preference
 * dependencies.
 */
public class MaterialYouColorSheet extends Dialog {

    private View contentView;
    private boolean isDismissing = false;

    public MaterialYouColorSheet(@NonNull Context context) {
        super(createThemedContext(context, R.style.FloatingBottomSheetTheme), 0);
    }

    private static Context createThemedContext(Context context, int themeResId) {
        Context baseContext = new ContextThemeWrapper(context, themeResId);
        return MaterialYouManager.getInstance(context).wrapContext(baseContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (window != null) {
            // Make window transparent
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set window attributes
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.5f;

            // Check blur preferences
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("keyboard_gpt_ui",
                    Context.MODE_PRIVATE);
            boolean isBlurEnabled = prefs.getBoolean("blur_enabled", true);
            int blurIntensity = prefs.getInt("blur_intensity", 25);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isBlurEnabled && blurIntensity > 0) {
                    // Calculate actual blur radius (max 50 for performance)
                    int blurRadius = (blurIntensity * 50) / 100;
                    params.setBlurBehindRadius(blurRadius);
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                } else {
                    params.setBlurBehindRadius(0);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
            }

            window.setAttributes(params);

            // Edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(false);
            }

            // Set navigation bar icons color logic (standard)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                if (!BottomSheetHelper.isDarkMode(getContext())) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    @Override
    public void setContentView(@NonNull View view) {
        this.contentView = view;

        // Apply theme to the content
        BottomSheetHelper.applyTheme(getContext(), view);

        // Create wrapper with margins for "Floating" look
        Context context = getContext();
        int margin = (int) (16 * context.getResources().getDisplayMetrics().density);
        int navBarHeight = getNavigationBarHeight(context);

        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        wrapper.setOnClickListener(v -> dismiss());
        view.setClickable(true);

        android.widget.FrameLayout.LayoutParams contentParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.BOTTOM;
        contentParams.setMargins(margin, 0, margin, navBarHeight + margin);

        wrapper.addView(view, contentParams);

        super.setContentView(wrapper);
    }

    @Override
    public void show() {
        super.show();
        // Animate in
        if (contentView != null) {
            Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            contentView.startAnimation(slideUp);
        }
    }

    @Override
    public void dismiss() {
        if (isDismissing)
            return;
        isDismissing = true;

        if (contentView != null) {
            Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    MaterialYouColorSheet.super.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            contentView.startAnimation(slideDown);
        } else {
            super.dismiss();
        }
    }

    private int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
