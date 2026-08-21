/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloadOptions;
import tn.eluea.kgpt.features.downloader.core.DownloaderPrefs;
import tn.eluea.kgpt.features.downloader.core.MediaSearchItem;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;
import tn.eluea.kgpt.features.downloader.core.ThumbnailLoader;
import tn.eluea.kgpt.features.downloader.service.MediaDownloadService;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class LinkSelectionBottomSheet {

    public interface OnLinkSelectedListener {
        void onLinkSelected(String url);
    }

    public interface OnMultipleLinksSelectedListener {
        void onLinksSelected(List<String> urls);
    }

    private final Context context;
    private final List<MediaSearchItem> items = new ArrayList<>();
    private final OnLinkSelectedListener singleListener;
    private OnMultipleLinksSelectedListener multiListener;
    private FloatingBottomSheet dialog;

    private MaterialCheckBox cbSelectAll;
    private TextView tvSelectedCounter;
    private MaterialButton btnDownloadSelected;
    private RecyclerView rvLinksList;
    private MultiLinkAdapter adapter;
    private DialogInterface.OnDismissListener onDismissListener;

    public LinkSelectionBottomSheet(@NonNull Context context, @NonNull List<String> urls, @NonNull OnLinkSelectedListener listener) {
        this.context = context;
        this.singleListener = listener;
        initItemsFromUrls(urls);
    }

    public LinkSelectionBottomSheet(@NonNull Context context, @NonNull List<String> urls, @NonNull OnMultipleLinksSelectedListener listener) {
        this.context = context;
        this.singleListener = null;
        this.multiListener = listener;
        initItemsFromUrls(urls);
    }

    private void initItemsFromUrls(List<String> urls) {
        if (urls != null) {
            for (String u : urls) {
                if (u == null || u.trim().isEmpty()) continue;
                String platform = MediaUtils.getPlatformName(u);
                String thumb = MediaUtils.getThumbnailUrl(u);
                String title = platform + " Media";
                String videoId = MediaUtils.extractYouTubeVideoId(u);
                if (videoId != null) {
                    title = "YouTube Video (" + videoId + ")";
                }
                MediaSearchItem item = new MediaSearchItem(videoId, title, u.trim(), thumb, "", "", platform);
                item.setSelected(true);
                items.add(item);
            }
        }
    }

    public void show() {
        if (items.isEmpty()) {
            return;
        }

        dialog = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_multi_link_select, null);
        BottomSheetHelper.applyTheme(context, sheetView);
        dialog.setContentView(sheetView);

        initViews(sheetView);
        setupRecyclerView();
        setupListeners(sheetView);
        updateSelectionCounter();

        if (onDismissListener != null) {
            dialog.setOnDismissListener(onDismissListener);
        }

        dialog.show();
    }

    private void initViews(View view) {
        TextView tvDesc = view.findViewById(R.id.tv_sheet_desc);
        if (tvDesc != null) {
            tvDesc.setText(context.getString(R.string.desc_select_links, items.size()));
        }

        cbSelectAll = view.findViewById(R.id.cb_select_all);
        tvSelectedCounter = view.findViewById(R.id.tv_selected_counter);
        btnDownloadSelected = view.findViewById(R.id.btn_download_selected);
        rvLinksList = view.findViewById(R.id.rv_links_list);
    }

    private void setupRecyclerView() {
        rvLinksList.setLayoutManager(new LinearLayoutManager(context));
        adapter = new MultiLinkAdapter(items, this::onItemToggled);
        rvLinksList.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        View btnClose = view.findViewById(R.id.btn_close_sheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if (dialog != null) {
                    dialog.dismiss();
                }
            });
        }

        if (cbSelectAll != null) {
            cbSelectAll.setOnClickListener(v -> {
                boolean checked = cbSelectAll.isChecked();
                for (MediaSearchItem item : items) {
                    item.setSelected(checked);
                }
                adapter.notifyDataSetChanged();
                updateSelectionCounter();
            });
        }

        if (btnDownloadSelected != null) {
            btnDownloadSelected.setOnClickListener(v -> performDownloadAction());
        }
    }

    private void onItemToggled(MediaSearchItem item, int position) {
        item.setSelected(!item.isSelected());
        adapter.notifyItemChanged(position);
        updateSelectionCounter();
    }

    private void updateSelectionCounter() {
        int selectedCount = 0;
        for (MediaSearchItem item : items) {
            if (item.isSelected()) {
                selectedCount++;
            }
        }

        if (tvSelectedCounter != null) {
            tvSelectedCounter.setText(context.getString(R.string.selected_counter, selectedCount, items.size()));
        }

        if (cbSelectAll != null) {
            cbSelectAll.setChecked(selectedCount == items.size());
        }

        if (btnDownloadSelected != null) {
            if (selectedCount > 0) {
                btnDownloadSelected.setEnabled(true);
                btnDownloadSelected.setText(context.getString(R.string.btn_download_selected, selectedCount));
            } else {
                btnDownloadSelected.setEnabled(false);
                btnDownloadSelected.setText(context.getString(R.string.btn_download_single));
            }
        }
    }

    private void performDownloadAction() {
        List<String> selectedUrls = new ArrayList<>();
        for (MediaSearchItem item : items) {
            if (item.isSelected()) {
                selectedUrls.add(item.getUrl());
            }
        }

        if (selectedUrls.isEmpty()) {
            Toast.makeText(context, R.string.toast_no_items_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismissInstant();
        }

        if (selectedUrls.size() == 1) {
            // If only 1 link is selected, open the full downloader configuration bottom sheet
            if (singleListener != null) {
                singleListener.onLinkSelected(selectedUrls.get(0));
            } else {
                MediaDownloaderBottomSheet sheet = new MediaDownloaderBottomSheet(context, selectedUrls.get(0));
                if (onDismissListener != null) {
                    sheet.setOnDismissListener(onDismissListener);
                }
                sheet.show();
            }
        } else {
            // Multiple links selected
            if (multiListener != null) {
                multiListener.onLinksSelected(selectedUrls);
            } else {
                // Batch download using user default preferences
                Toast.makeText(context, context.getString(R.string.batch_download_started, selectedUrls.size()), Toast.LENGTH_SHORT).show();
                for (String url : selectedUrls) {
                    DownloadOptions options = new DownloadOptions(url);
                    options.setType(DownloadOptions.Type.VIDEO);
                    options.setVideoQuality(DownloaderPrefs.getDefaultVideoQuality(context));
                    options.setVideoFormat(DownloaderPrefs.getDefaultVideoFormat(context));
                    options.setEmbedThumbnail(DownloaderPrefs.isEmbedThumbnail(context));
                    options.setEmbedMetadata(DownloaderPrefs.isEmbedMetadata(context));
                    options.setEmbedSubtitles(DownloaderPrefs.isEmbedSubtitles(context));
                    options.setSplitChapters(DownloaderPrefs.isSplitChapters(context));

                    String title = MediaUtils.getPlatformName(url) + " Media";
                    MediaDownloadService.startDownload(context, options, title);
                }
            }
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.onDismissListener = listener;
        if (dialog != null) {
            dialog.setOnDismissListener(listener);
        }
    }

    public FloatingBottomSheet getDialog() {
        return dialog;
    }

    // --- Multi Link Adapter ---
    private static class MultiLinkAdapter extends RecyclerView.Adapter<MultiLinkAdapter.ViewHolder> {
        private final List<MediaSearchItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(MediaSearchItem item, int position);
        }

        MultiLinkAdapter(List<MediaSearchItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multi_link_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaSearchItem item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvUrl.setText(item.getUrl());
            holder.tvPlatformTag.setText(item.getPlatform());
            holder.checkBox.setChecked(item.isSelected());

            // Load thumbnail or platform icon
            String thumb = item.getThumbnailUrl();
            if (thumb != null && !thumb.isEmpty()) {
                ThumbnailLoader.getInstance().load(thumb, holder.ivThumbnail, R.drawable.ic_movie_outline);
            } else {
                if ("YouTube".equals(item.getPlatform())) {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_movie_outline);
                } else if ("SoundCloud".equals(item.getPlatform())) {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_lamp_charge_filled);
                } else {
                    holder.ivThumbnail.setImageResource(R.drawable.ic_download_filled);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item, holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCheckBox checkBox;
            final ImageView ivThumbnail;
            final TextView tvPlatformTag;
            final TextView tvTitle;
            final TextView tvUrl;

            ViewHolder(View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.cb_link_select);
                ivThumbnail = itemView.findViewById(R.id.iv_link_thumbnail);
                tvPlatformTag = itemView.findViewById(R.id.tv_platform_tag);
                tvTitle = itemView.findViewById(R.id.tv_link_title);
                tvUrl = itemView.findViewById(R.id.tv_link_url);
            }
        }
    }
}
