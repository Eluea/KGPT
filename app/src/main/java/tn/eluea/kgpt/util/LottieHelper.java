package tn.eluea.kgpt.util;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;

public class LottieHelper {

    public static void tint(LottieAnimationView view, int color) {
        if (view == null) return;
        PorterDuffColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        
        // Remove previous callbacks for COLOR_FILTER to prevent accumulating conflicting colors
        view.removeAllLottieOnCompositionLoadedListener();

        view.addValueCallback(
                new KeyPath("**"),
                LottieProperty.COLOR_FILTER,
                frameInfo -> filter
        );

        if (view.getComposition() == null) {
            view.addLottieOnCompositionLoadedListener(composition -> {
                view.addValueCallback(
                        new KeyPath("**"),
                        LottieProperty.COLOR_FILTER,
                        frameInfo -> filter
                );
                view.invalidate();
            });
        }
        view.invalidate();
    }

    public static void setStaticFrame(LottieAnimationView view, int frame, int color) {
        if (view == null) return;
        view.cancelAnimation();
        view.pauseAnimation();
        view.setFrame(frame);
        tint(view, color);
        view.invalidate();
    }

    public static void playOnce(LottieAnimationView view, int color) {
        if (view == null) return;
        view.cancelAnimation();
        tint(view, color);
        view.setFrame(0);
        view.playAnimation();
    }
}
