package tn.eluea.kgpt.features.downloader.core;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaUtils {
    private static final String TAG = "KGPT_MediaUtils";

    // URL regex pattern to extract URLs from shared text
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    );

    /**
     * Extract the first valid URL from any text (such as shared text from YouTube/Instagram/TikTok).
     */
    public static String extractUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        java.util.List<String> all = extractAllUrls(text);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return text.trim();
    }

    /**
     * Extract all unique valid URLs from any text in chronological order.
     */
    public static java.util.List<String> extractAllUrls(String text) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return urls;
        }
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            if (url != null && !url.trim().isEmpty()) {
                // Strip common trailing punctuation
                url = url.replaceAll("[.,;)\\]>]+$", "").trim();
                if (!url.isEmpty() && !seen.contains(url)) {
                    seen.add(url);
                }
            }
        }
        urls.addAll(seen);
        return urls;
    }

    /**
     * Get user-friendly platform/domain name from URL.
     */
    public static String getPlatformName(String url) {
        if (url == null) return "Web Media";
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) return "YouTube";
        if (lower.contains("instagram.com")) return "Instagram";
        if (lower.contains("tiktok.com")) return "TikTok";
        if (lower.contains("twitter.com") || lower.contains("x.com")) return "X (Twitter)";
        if (lower.contains("facebook.com") || lower.contains("fb.watch")) return "Facebook";
        if (lower.contains("soundcloud.com")) return "SoundCloud";
        if (lower.contains("reddit.com")) return "Reddit";
        if (lower.contains("vimeo.com")) return "Vimeo";
        if (lower.contains("pinterest.com")) return "Pinterest";
        if (lower.contains("bilibili.com")) return "Bilibili";
        if (lower.contains("twitch.tv")) return "Twitch";
        if (lower.contains("threads.net")) return "Threads";
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) host = host.substring(4);
                return host;
            }
        } catch (Throwable ignored) {}
        return "Web Media";
    }

    /**
     * Get default download directory for KGPT media (Downloads/KGPT).
     */
    public static File getDefaultDownloadDir(boolean isAudio) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File kgptDir = new File(downloadsDir, isAudio ? "KGPT/Audio" : "KGPT/Video");
        if (!kgptDir.exists()) {
            kgptDir.mkdirs();
        }
        return kgptDir;
    }

    /**
     * Scan file into Android MediaStore so it immediately appears in Gallery/Music players.
     */
    public static void scanMediaFile(Context context, File file, String mimeType) {
        if (context == null || file == null || !file.exists()) {
            return;
        }
        try {
            MediaScannerConnection.scanFile(
                    context.getApplicationContext(),
                    new String[]{file.getAbsolutePath()},
                    mimeType != null ? new String[]{mimeType} : null,
                    (path, uri) -> Log.d(TAG, "Scanned into MediaStore: " + path + " -> " + uri)
            );
        } catch (Throwable t) {
            Log.e(TAG, "Failed to scan media file: " + t.getMessage());
        }
    }

    /**
     * Format bytes into human readable format (MB, GB, KB).
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "حجم غير محدد";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Format duration seconds into mm:ss or hh:mm:ss.
     */
    public static String formatDuration(long seconds) {
        if (seconds < 0) return "--:--";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s);
        } else {
            return String.format(java.util.Locale.US, "%02d:%02d", m, s);
        }
    }

    /**
     * Extract 11-char YouTube Video ID from any YouTube URL format.
     */
    public static String extractYouTubeVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        Pattern ytPattern = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/|v/|live/))([a-zA-Z0-9_-]{11})");
        Matcher matcher = ytPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Get instant thumbnail image URL for known platforms like YouTube.
     */
    public static String getThumbnailUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String videoId = extractYouTubeVideoId(url);
        if (videoId != null && !videoId.isEmpty()) {
            return "https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
        }
        return null;
    }

    /**
     * Check if a string is a search query rather than a direct web URL.
     */
    public static boolean isSearchQuery(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String clean = text.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return false;
        }
        return extractAllUrls(clean).isEmpty();
    }
}

