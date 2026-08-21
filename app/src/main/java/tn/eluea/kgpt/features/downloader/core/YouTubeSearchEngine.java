/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance YouTube search engine with InnerTube Web API and yt-dlp fallback.
 */
public class YouTubeSearchEngine {
    private static final String TAG = "KGPT_YouTubeSearch";
    private static YouTubeSearchEngine instance;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class SearchResult {
        private final List<MediaSearchItem> items;
        private final String continuationToken;
        private final boolean hasMore;

        public SearchResult(List<MediaSearchItem> items, String continuationToken, boolean hasMore) {
            this.items = items != null ? items : new ArrayList<>();
            this.continuationToken = continuationToken;
            this.hasMore = hasMore;
        }

        public List<MediaSearchItem> getItems() {
            return items;
        }

        public String getContinuationToken() {
            return continuationToken;
        }

        public boolean hasMore() {
            return hasMore;
        }
    }

    public interface SearchCallback {
        void onSuccess(SearchResult result);
        void onError(Exception e);
    }

    public static synchronized YouTubeSearchEngine getInstance() {
        if (instance == null) {
            instance = new YouTubeSearchEngine();
        }
        return instance;
    }

    private YouTubeSearchEngine() {
    }

    /**
     * Search YouTube with pagination support.
     */
    public void search(Context context, String query, int targetCount, String continuationToken, SearchCallback callback) {
        executor.execute(() -> {
            try {
                SearchResult result = null;
                // 1. Try YouTube InnerTube API first (Instant ~200ms)
                try {
                    result = searchInnerTube(query, targetCount, continuationToken);
                } catch (Exception e) {
                    Log.w(TAG, "InnerTube search failed, falling back to yt-dlp: " + e.getMessage());
                }

                // 2. Fallback to yt-dlp if InnerTube returned no items
                if (result == null || result.getItems().isEmpty()) {
                    result = searchViaYtdlp(context, query, targetCount > 0 ? targetCount : 20);
                }

                final SearchResult finalResult = result;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(finalResult);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Search execution error: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Search via YouTube InnerTube Web API.
     */
    private SearchResult searchInnerTube(String query, int targetCount, String continuationToken) throws Exception {
        URL url = new URL("https://www.youtube.com/youtubei/v1/search?prettyPrint=false");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("X-YouTube-Client-Name", "1");
        conn.setRequestProperty("X-YouTube-Client-Version", "2.20240101.01.00");

        JSONObject rootRequest = new JSONObject();
        JSONObject context = new JSONObject();
        JSONObject client = new JSONObject();
        client.put("hl", "ar");
        client.put("gl", "US");
        client.put("clientName", "WEB");
        client.put("clientVersion", "2.20240101.01.00");
        context.put("client", client);
        rootRequest.put("context", context);

        if (continuationToken != null && !continuationToken.trim().isEmpty()) {
            rootRequest.put("continuation", continuationToken.trim());
        } else {
            rootRequest.put("query", query);
        }

        byte[] postBytes = rootRequest.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postBytes);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("InnerTube HTTP " + responseCode);
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        JSONObject json = new JSONObject(responseBuilder.toString());
        List<MediaSearchItem> items = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        String nextContinuation = null;

        if (json.has("contents")) {
            JSONObject contents = json.optJSONObject("contents");
            if (contents != null) {
                JSONObject twoCol = contents.optJSONObject("twoColumnSearchResultsRenderer");
                if (twoCol != null) {
                    JSONObject primary = twoCol.optJSONObject("primaryContents");
                    if (primary != null) {
                        JSONObject sectionList = primary.optJSONObject("sectionListRenderer");
                        if (sectionList != null) {
                            JSONArray sectionContents = sectionList.optJSONArray("contents");
                            if (sectionContents != null) {
                                for (int i = 0; i < sectionContents.length(); i++) {
                                    JSONObject section = sectionContents.optJSONObject(i);
                                    if (section == null) continue;

                                    JSONObject itemSection = section.optJSONObject("itemSectionRenderer");
                                    if (itemSection != null) {
                                        JSONArray itemContents = itemSection.optJSONArray("contents");
                                        if (itemContents != null) {
                                            parseVideoRenderers(itemContents, items, seenIds, targetCount);
                                        }
                                    }

                                    JSONObject continuationItem = section.optJSONObject("continuationItemRenderer");
                                    if (continuationItem != null) {
                                        nextContinuation = extractContinuationToken(continuationItem);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (json.has("onResponseReceivedCommands")) {
            JSONArray commands = json.optJSONArray("onResponseReceivedCommands");
            if (commands != null) {
                for (int c = 0; c < commands.length(); c++) {
                    JSONObject cmd = commands.optJSONObject(c);
                    if (cmd == null) continue;
                    JSONObject appendAction = cmd.optJSONObject("appendContinuationItemsAction");
                    if (appendAction != null) {
                        JSONArray contItems = appendAction.optJSONArray("continuationItems");
                        if (contItems != null) {
                            parseVideoRenderers(contItems, items, seenIds, targetCount);
                            for (int k = 0; k < contItems.length(); k++) {
                                JSONObject cItem = contItems.optJSONObject(k);
                                if (cItem != null && cItem.has("continuationItemRenderer")) {
                                    nextContinuation = extractContinuationToken(cItem.optJSONObject("continuationItemRenderer"));
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean hasMore = (nextContinuation != null && !nextContinuation.isEmpty()) || items.size() > 5;
        return new SearchResult(items, nextContinuation, hasMore);
    }

    private void parseVideoRenderers(JSONArray array, List<MediaSearchItem> items, Set<String> seenIds, int maxCount) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            if (maxCount > 0 && items.size() >= maxCount) {
                break;
            }
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;

            JSONObject videoRenderer = obj.optJSONObject("videoRenderer");
            if (videoRenderer != null) {
                String videoId = videoRenderer.optString("videoId", "");
                if (videoId.isEmpty() || !seenIds.add(videoId)) continue;

                String title = extractText(videoRenderer.optJSONObject("title"));
                String duration = extractText(videoRenderer.optJSONObject("lengthText"));
                String uploader = extractText(videoRenderer.optJSONObject("ownerText"));
                if (uploader.isEmpty()) {
                    uploader = extractText(videoRenderer.optJSONObject("longBylineText"));
                }
                String viewCount = extractText(videoRenderer.optJSONObject("viewCountText"));
                if (viewCount.isEmpty()) {
                    viewCount = extractText(videoRenderer.optJSONObject("shortViewCountText"));
                }

                String thumbUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                JSONObject thumbObj = videoRenderer.optJSONObject("thumbnail");
                if (thumbObj != null) {
                    JSONArray thumbs = thumbObj.optJSONArray("thumbnails");
                    if (thumbs != null && thumbs.length() > 0) {
                        JSONObject bestThumb = thumbs.optJSONObject(thumbs.length() - 1);
                        if (bestThumb != null && bestThumb.has("url")) {
                            String rawUrl = bestThumb.optString("url");
                            if (rawUrl != null && !rawUrl.isEmpty()) {
                                if (rawUrl.startsWith("//")) {
                                    rawUrl = "https:" + rawUrl;
                                }
                                thumbUrl = rawUrl;
                            }
                        }
                    }
                }

                String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
                MediaSearchItem item = new MediaSearchItem(
                        videoId,
                        title.isEmpty() ? "YouTube Video" : title,
                        videoUrl,
                        thumbUrl,
                        duration,
                        uploader,
                        "YouTube"
                );
                item.setViewCount(viewCount);
                items.add(item);
            }
        }
    }

    private String extractText(JSONObject textObj) {
        if (textObj == null) return "";
        if (textObj.has("simpleText")) {
            return textObj.optString("simpleText", "");
        }
        if (textObj.has("runs")) {
            JSONArray runs = textObj.optJSONArray("runs");
            if (runs != null && runs.length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < runs.length(); r++) {
                    JSONObject run = runs.optJSONObject(r);
                    if (run != null && run.has("text")) {
                        sb.append(run.optString("text"));
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    private String extractContinuationToken(JSONObject continuationItem) {
        if (continuationItem == null) return null;
        JSONObject endpoint = continuationItem.optJSONObject("continuationEndpoint");
        if (endpoint != null) {
            JSONObject cmd = endpoint.optJSONObject("continuationCommand");
            if (cmd != null) {
                return cmd.optString("token", null);
            }
        }
        return null;
    }

    /**
     * Fallback search via yt-dlp engine.
     */
    private SearchResult searchViaYtdlp(Context context, String query, int targetCount) {
        List<MediaSearchItem> items = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        try {
            DownloaderEngine.getInstance().init(context);
            int count = targetCount > 0 ? targetCount : 20;
            YoutubeDLRequest request = new YoutubeDLRequest("ytsearch" + count + ":" + query);
            request.addOption("--flat-playlist");
            request.addOption("--dump-json");
            request.addOption("--no-warnings");
            request.addOption("--ignore-errors");
            request.addOption("--skip-download");
            request.addOption("--no-update");
            request.addOption("--extractor-args", "youtube:player_client=android,web");

            YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, null, null);
            String out = response.getOut();
            if (out != null && !out.isEmpty()) {
                String[] lines = out.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || !line.startsWith("{")) continue;
                    try {
                        JSONObject obj = new JSONObject(line);
                        String id = obj.optString("id");
                        if (id.isEmpty() || !seenIds.add(id)) continue;

                        String title = obj.optString("title", "YouTube Video");
                        String url = obj.optString("url");
                        if (url == null || url.isEmpty() || !url.startsWith("http")) {
                            url = "https://www.youtube.com/watch?v=" + id;
                        }
                        String durationSec = obj.optString("duration");
                        String durationStr = "";
                        try {
                            if (!durationSec.isEmpty()) {
                                long sec = (long) Double.parseDouble(durationSec);
                                durationStr = MediaUtils.formatDuration(sec);
                            }
                        } catch (Throwable ignored) {}

                        String uploader = obj.optString("uploader");
                        if (uploader.isEmpty()) uploader = obj.optString("channel");
                        String thumb = "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";

                        MediaSearchItem item = new MediaSearchItem(
                                id,
                                title,
                                url,
                                thumb,
                                durationStr,
                                uploader,
                                "YouTube"
                        );
                        items.add(item);
                    } catch (Throwable t) {
                        Log.w(TAG, "Failed to parse yt-dlp json line: " + t.getMessage());
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "yt-dlp search error: " + t.getMessage());
        }
        return new SearchResult(items, null, items.size() >= targetCount);
    }
}
