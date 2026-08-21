package tn.eluea.kgpt.features.downloader.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.yausername.youtubedl_android.YoutubeDL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.features.downloader.core.DownloaderPrefs;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class CoreUpdateBottomSheet {

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FloatingBottomSheet dialog;

    private com.airbnb.lottie.LottieAnimationView lottieProgress;
    private ImageView ivStatus;
    private TextView tvTitle;
    private TextView tvMessage;
    private MaterialButton btnAction;

    public CoreUpdateBottomSheet(@NonNull Context context) {
        this.context = context;
    }

    public void show() {
        dialog = new FloatingBottomSheet(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_core_update, null);
        BottomSheetHelper.applyTheme(context, view);
        dialog.setContentView(view);

        lottieProgress = view.findViewById(R.id.lottie_progress);
        ivStatus = view.findViewById(R.id.iv_status);
        tvTitle = view.findViewById(R.id.tv_title);
        tvMessage = view.findViewById(R.id.tv_message);
        btnAction = view.findViewById(R.id.btn_action);

        int primaryColor = com.google.android.material.color.MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorPrimary, android.graphics.Color.WHITE);
        if (lottieProgress != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieProgress, primaryColor);
        }

        btnAction.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> executor.shutdown());
        dialog.show();

        performUpdateCheck();
    }

    private void performUpdateCheck() {
        DownloaderEngine.getInstance().init(context);
        executor.execute(() -> {
            try {
                YoutubeDL.UpdateStatus status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE);
                DownloaderPrefs.setCoreInstalled(context, true);
                try {
                    java.util.List<tn.eluea.kgpt.text.parse.ParsePattern> patterns = tn.eluea.kgpt.SPManager.getInstance().getParsePatterns();
                    boolean updated = false;
                    for (int i = 0; i < patterns.size(); i++) {
                        tn.eluea.kgpt.text.parse.ParsePattern p = patterns.get(i);
                        if (p.getType() == tn.eluea.kgpt.text.parse.PatternType.MediaDownloader && !p.isEnabled()) {
                            patterns.set(i, p.withEnabled(true));
                            updated = true;
                        }
                    }
                    if (updated) {
                        tn.eluea.kgpt.SPManager.getInstance().setParsePatterns(patterns);
                    }

                    // Also auto-enable TextAction.DOWNLOAD
                    tn.eluea.kgpt.features.textactions.data.TextActionManager actionManager =
                            new tn.eluea.kgpt.features.textactions.data.TextActionManager(context);
                    java.util.List<tn.eluea.kgpt.features.textactions.domain.TextAction> actions =
                            new java.util.ArrayList<>(actionManager.getEnabledActions());
                    if (!actions.contains(tn.eluea.kgpt.features.textactions.domain.TextAction.DOWNLOAD)) {
                        actions.add(tn.eluea.kgpt.features.textactions.domain.TextAction.DOWNLOAD);
                        android.content.SharedPreferences prefs = context.getSharedPreferences("keyboard_gpt", android.content.Context.MODE_PRIVATE);
                        prefs.edit().putString("text_actions_list", tn.eluea.kgpt.features.textactions.data.TextActionManager.encodeEnabledActions(actions)).apply();
                    }
                } catch (Throwable ignored) {}
                mainHandler.post(() -> {
                    if (lottieProgress != null) lottieProgress.setVisibility(View.GONE);
                    if (ivStatus != null) {
                        ivStatus.setVisibility(View.VISIBLE);
                        ivStatus.setImageResource(R.drawable.ic_shield_tick_filled);
                    }
                    if (tvMessage != null) {
                        if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                            tvMessage.setText(context.getString(R.string.status_core_up_to_date));
                        } else {
                            tvMessage.setText(context.getString(R.string.status_core_updated));
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (lottieProgress != null) lottieProgress.setVisibility(View.GONE);
                    if (ivStatus != null) {
                        ivStatus.setVisibility(View.VISIBLE);
                        ivStatus.setImageResource(R.drawable.ic_shield_cross_filled);
                    }
                    if (tvMessage != null) {
                        tvMessage.setText(e.getMessage() != null ? e.getMessage() : context.getString(R.string.error_extract_failed));
                    }
                });
            }
        });
    }
}
