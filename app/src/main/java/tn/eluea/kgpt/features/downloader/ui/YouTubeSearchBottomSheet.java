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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.MediaSearchItem;
import tn.eluea.kgpt.features.downloader.core.ThumbnailLoader;
import tn.eluea.kgpt.features.downloader.core.YouTubeSearchEngine;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.util.LottieHelper;

public class YouTubeSearchBottomSheet {

    private final Context context;
    private final String searchQuery;
    private FloatingBottomSheet dialog;

    private View layoutInitialLoading;
    private View layoutEmptyState;
    private View layoutResultsContainer;
    private View containerLoadMore;
    private View layoutButtonLoading;
    private MaterialButton btnLoadMore;
    private LottieAnimationView lottieSearchSpinner;
    private LottieAnimationView lottieBtnSpinner;
    private TextView tvSearchQueryDesc;
    private TextView tvEmptyMessage;
    private RecyclerView rvSearchResults;

    private final List<MediaSearchItem> searchItems = new ArrayList<>();
    private final List<MediaSearchItem> bufferedItems = new ArrayList<>();
    private final Set<String> seenVideoIds = new HashSet<>();
    private SearchResultsAdapter adapter;

    private String continuationToken = null;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private DialogInterface.OnDismissListener onDismissListener;

    public YouTubeSearchBottomSheet(@NonNull Context context, @NonNull String query) {
        this.context = context;
        this.searchQuery = query != null ? query.trim() : "";
    }

    public void show() {
        if (searchQuery.isEmpty()) {
            Toast.makeText(context, R.string.toast_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_youtube_search, null);
        BottomSheetHelper.applyTheme(context, sheetView);
        dialog.setContentView(sheetView);

        initViews(sheetView);
        setupRecyclerView();
        setupListeners(sheetView);

        if (onDismissListener != null) {
            dialog.setOnDismissListener(onDismissListener);
        }

        dialog.show();

        // Perform initial search
        loadInitialSearchResults();
    }

    private void initViews(View view) {
        layoutInitialLoading = view.findViewById(R.id.layout_initial_loading);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutResultsContainer = view.findViewById(R.id.layout_results_container);
        containerLoadMore = view.findViewById(R.id.container_load_more);
        layoutButtonLoading = view.findViewById(R.id.layout_button_loading);
        btnLoadMore = view.findViewById(R.id.btn_load_more_results);
        lottieSearchSpinner = view.findViewById(R.id.lottie_search_spinner);
        lottieBtnSpinner = view.findViewById(R.id.lottie_btn_spinner);
        tvSearchQueryDesc = view.findViewById(R.id.tv_search_query_desc);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        rvSearchResults = view.findViewById(R.id.rv_search_results);

        int primaryColor = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
        if (lottieSearchSpinner != null) {
            LottieHelper.tint(lottieSearchSpinner, primaryColor);
        }
        if (lottieBtnSpinner != null) {
            LottieHelper.tint(lottieBtnSpinner, primaryColor);
        }

        if (tvSearchQueryDesc != null) {
            tvSearchQueryDesc.setText(context.getString(R.string.desc_youtube_search, searchQuery));
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvSearchResults.setLayoutManager(layoutManager);
        adapter = new SearchResultsAdapter(searchItems, this::onVideoSelected);
        rvSearchResults.setAdapter(adapter);

        // Infinite scrolling: load next 5 results when scrolling near the end
        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMore) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 1) {
                            loadMoreSearchResults();
                        }
                    }
                }
            }
        });
    }

    private void setupListeners(View view) {
        View btnClose = view.findViewById(R.id.btn_close_search);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if (dialog != null) {
                    dialog.dismiss();
                }
            });
        }

        if (btnLoadMore != null) {
            btnLoadMore.setOnClickListener(v -> loadMoreSearchResults());
        }
    }

    private void onVideoSelected(MediaSearchItem item) {
        if (dialog != null) {
            // Clear dismiss listener before dismiss to avoid prematurely finishing the host activity
            dialog.setOnDismissListener(null);
            dialog.dismissInstant();
        }

        // Open full downloader bottom sheet for the selected video
        MediaDownloaderBottomSheet downloaderSheet = new MediaDownloaderBottomSheet(context, item.getUrl());
        if (onDismissListener != null) {
            downloaderSheet.setOnDismissListener(onDismissListener);
        }
        downloaderSheet.show();
    }

    private void loadInitialSearchResults() {
        isLoading = true;
        layoutInitialLoading.setVisibility(View.VISIBLE);
        layoutResultsContainer.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);

        if (lottieSearchSpinner != null) {
            lottieSearchSpinner.playAnimation();
        }

        YouTubeSearchEngine.getInstance().search(context, searchQuery, 0, null, new YouTubeSearchEngine.SearchCallback() {
            @Override
            public void onSuccess(YouTubeSearchEngine.SearchResult result) {
                isLoading = false;
                layoutInitialLoading.setVisibility(View.GONE);

                if (result != null && !result.getItems().isEmpty()) {
                    searchItems.clear();
                    bufferedItems.clear();
                    seenVideoIds.clear();

                    for (MediaSearchItem item : result.getItems()) {
                        if (seenVideoIds.add(item.getId())) {
                            bufferedItems.add(item);
                        }
                    }

                    continuationToken = result.getContinuationToken();

                    // Take initial 5 items from the buffer
                    int initialCount = Math.min(5, bufferedItems.size());
                    for (int i = 0; i < initialCount; i++) {
                        searchItems.add(bufferedItems.get(i));
                    }

                    hasMore = (bufferedItems.size() > searchItems.size()) || result.hasMore();

                    layoutResultsContainer.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();

                    if (containerLoadMore != null) {
                        containerLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                    }
                    if (layoutButtonLoading != null) {
                        layoutButtonLoading.setVisibility(View.GONE);
                    }
                } else {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    if (tvEmptyMessage != null) {
                        tvEmptyMessage.setText(context.getString(R.string.no_search_results, searchQuery));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                layoutInitialLoading.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                if (tvEmptyMessage != null) {
                    tvEmptyMessage.setText(context.getString(R.string.no_search_results, searchQuery));
                }
            }
        });
    }

    private void loadMoreSearchResults() {
        if (isLoading || !hasMore) {
            return;
        }

        // 1. If buffer has more items that are not yet displayed, serve next 5 immediately!
        if (searchItems.size() < bufferedItems.size()) {
            int fromIndex = searchItems.size();
            int toIndex = Math.min(fromIndex + 5, bufferedItems.size());
            int countAdded = toIndex - fromIndex;

            for (int i = fromIndex; i < toIndex; i++) {
                searchItems.add(bufferedItems.get(i));
            }

            hasMore = (bufferedItems.size() > searchItems.size()) || (continuationToken != null && !continuationToken.isEmpty());
            adapter.notifyItemRangeInserted(fromIndex, countAdded);

            if (containerLoadMore != null) {
                containerLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
            }
            return;
        }

        // 2. Buffer is exhausted, fetch next page from YouTube
        isLoading = true;
        if (layoutButtonLoading != null) {
            layoutButtonLoading.setVisibility(View.VISIBLE);
            if (lottieBtnSpinner != null) {
                lottieBtnSpinner.playAnimation();
            }
        }
        if (btnLoadMore != null) {
            btnLoadMore.setEnabled(false);
        }

        YouTubeSearchEngine.getInstance().search(context, searchQuery, 20, continuationToken, new YouTubeSearchEngine.SearchCallback() {
            @Override
            public void onSuccess(YouTubeSearchEngine.SearchResult result) {
                isLoading = false;
                if (layoutButtonLoading != null) {
                    layoutButtonLoading.setVisibility(View.GONE);
                }
                if (btnLoadMore != null) {
                    btnLoadMore.setEnabled(true);
                }

                if (result != null && !result.getItems().isEmpty()) {
                    continuationToken = result.getContinuationToken();
                    int addedToBuffer = 0;
                    for (MediaSearchItem item : result.getItems()) {
                        if (seenVideoIds.add(item.getId())) {
                            bufferedItems.add(item);
                            addedToBuffer++;
                        }
                    }

                    if (addedToBuffer > 0 && searchItems.size() < bufferedItems.size()) {
                        int fromIndex = searchItems.size();
                        int toIndex = Math.min(fromIndex + 5, bufferedItems.size());
                        int countAdded = toIndex - fromIndex;

                        for (int i = fromIndex; i < toIndex; i++) {
                            searchItems.add(bufferedItems.get(i));
                        }

                        hasMore = (bufferedItems.size() > searchItems.size()) || result.hasMore();
                        adapter.notifyItemRangeInserted(fromIndex, countAdded);

                        if (containerLoadMore != null) {
                            containerLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                        }
                    } else {
                        hasMore = false;
                        if (containerLoadMore != null) {
                            containerLoadMore.setVisibility(View.GONE);
                        }
                    }
                } else {
                    hasMore = false;
                    if (containerLoadMore != null) {
                        containerLoadMore.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                if (layoutButtonLoading != null) {
                    layoutButtonLoading.setVisibility(View.GONE);
                }
                if (btnLoadMore != null) {
                    btnLoadMore.setEnabled(true);
                }
            }
        });
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

    // --- Search Results Adapter ---
    private static class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
        private final List<MediaSearchItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(MediaSearchItem item);
        }

        SearchResultsAdapter(List<MediaSearchItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_youtube_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaSearchItem item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvUploader.setText(item.getUploader());

            if (item.getDuration() != null && !item.getDuration().isEmpty()) {
                holder.tvDuration.setVisibility(View.VISIBLE);
                holder.tvDuration.setText(item.getDuration());
            } else {
                holder.tvDuration.setVisibility(View.GONE);
            }

            if (item.getViewCount() != null && !item.getViewCount().isEmpty()) {
                holder.tvViews.setVisibility(View.VISIBLE);
                holder.tvViews.setText(item.getViewCount());
            } else {
                holder.tvViews.setVisibility(View.GONE);
            }

            // Load colored thumbnail with smooth fade-in
            ThumbnailLoader.getInstance().load(item.getThumbnailUrl(), holder.ivThumbnail, R.drawable.ic_movie_outline);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            if (holder.btnDownload != null) {
                holder.btnDownload.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(item);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivThumbnail;
            final TextView tvDuration;
            final TextView tvTitle;
            final TextView tvUploader;
            final TextView tvViews;
            final View btnDownload;

            ViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_search_thumbnail);
                tvDuration = itemView.findViewById(R.id.tv_search_duration);
                tvTitle = itemView.findViewById(R.id.tv_search_title);
                tvUploader = itemView.findViewById(R.id.tv_search_uploader);
                tvViews = itemView.findViewById(R.id.tv_search_views);
                btnDownload = itemView.findViewById(R.id.btn_item_download);
            }
        }
    }
}
