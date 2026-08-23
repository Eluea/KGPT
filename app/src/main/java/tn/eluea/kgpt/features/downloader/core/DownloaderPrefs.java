package tn.eluea.kgpt.features.downloader.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

public class DownloaderPrefs {
    private static final String PREF_NAME = "kgpt_downloader_prefs";

    public static final String KEY_CUSTOM_DOWNLOAD_DIR = "custom_download_dir";
    public static final String KEY_GROUP_BY_UPLOADER = "group_by_uploader";
    public static final String KEY_SEPARATE_AUDIO_VIDEO = "separate_audio_video";
    public static final String KEY_DEFAULT_TYPE = "default_type"; // "video" or "audio"
    public static final String KEY_DEFAULT_VIDEO_QUALITY = "default_video_quality"; // "best", "2160", "1440", "1080", "720", "480"
    public static final String KEY_DEFAULT_VIDEO_FORMAT = "default_video_format"; // "mp4", "mkv", "webm"
    public static final String KEY_DEFAULT_AUDIO_FORMAT = "default_audio_format"; // "mp3", "m4a", "flac", "opus"
    public static final String KEY_DEFAULT_AUDIO_BITRATE = "default_audio_bitrate"; // "0" (320k), "2" (256k), "5" (192k)
    public static final String KEY_EMBED_THUMBNAIL = "embed_thumbnail";
    public static final String KEY_EMBED_METADATA = "embed_metadata";
    public static final String KEY_EMBED_SUBTITLES = "embed_subtitles";
    public static final String KEY_SPLIT_CHAPTERS = "split_chapters";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getDownloadRootPath(Context context) {
        String custom = getPrefs(context).getString(KEY_CUSTOM_DOWNLOAD_DIR, null);
        if (custom != null && !custom.trim().isEmpty()) {
            File customFile = new File(custom);
            if (customFile.exists() || customFile.mkdirs()) {
                return customFile.getAbsolutePath();
            }
        }
        File defaultDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KGPT");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        return defaultDir.getAbsolutePath();
    }

    public static void setCustomDownloadPath(Context context, String path) {
        getPrefs(context).edit().putString(KEY_CUSTOM_DOWNLOAD_DIR, path).apply();
    }

    public static final String KEY_GROUP_BY_APP = "group_by_app";

    public static boolean isGroupByApp(Context context) {
        return getPrefs(context).getBoolean(KEY_GROUP_BY_APP, false);
    }

    public static void setGroupByApp(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_GROUP_BY_APP, value).apply();
    }

    public static boolean isGroupByUploader(Context context) {
        return getPrefs(context).getBoolean(KEY_GROUP_BY_UPLOADER, false);
    }

    public static void setGroupByUploader(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_GROUP_BY_UPLOADER, value).apply();
    }

    public static boolean isSeparateAudioVideo(Context context) {
        return getPrefs(context).getBoolean(KEY_SEPARATE_AUDIO_VIDEO, true);
    }

    public static void setSeparateAudioVideo(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_SEPARATE_AUDIO_VIDEO, value).apply();
    }

    public static File getTargetDownloadDirectory(Context context, boolean isAudio, String uploader) {
        return getTargetDownloadDirectory(context, isAudio, uploader, null);
    }

    public static File getTargetDownloadDirectory(Context context, boolean isAudio, String uploader, String sourceApp) {
        String rootPath = getDownloadRootPath(context);
        File targetDir = new File(rootPath);

        if (isSeparateAudioVideo(context)) {
            targetDir = new File(targetDir, isAudio ? "Audio" : "Video");
        }

        if (isGroupByApp(context) && sourceApp != null && !sourceApp.trim().isEmpty()) {
            String cleanApp = sourceApp.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            targetDir = new File(targetDir, cleanApp);
        }

        if (isGroupByUploader(context) && uploader != null && !uploader.trim().isEmpty()) {
            // Clean uploader folder name of illegal characters
            String cleanUploader = uploader.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            targetDir = new File(targetDir, cleanUploader);
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        return targetDir;
    }

    public static String getDefaultType(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_TYPE, "video");
    }

    public static void setDefaultType(Context context, String type) {
        getPrefs(context).edit().putString(KEY_DEFAULT_TYPE, type).apply();
    }

    public static String getDefaultVideoQuality(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_VIDEO_QUALITY, "1080");
    }

    public static void setDefaultVideoQuality(Context context, String quality) {
        getPrefs(context).edit().putString(KEY_DEFAULT_VIDEO_QUALITY, quality).apply();
    }

    public static String getDefaultVideoFormat(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_VIDEO_FORMAT, "mp4");
    }

    public static void setDefaultVideoFormat(Context context, String format) {
        getPrefs(context).edit().putString(KEY_DEFAULT_VIDEO_FORMAT, format).apply();
    }

    public static String getDefaultAudioFormat(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_AUDIO_FORMAT, "mp3");
    }

    public static void setDefaultAudioFormat(Context context, String format) {
        getPrefs(context).edit().putString(KEY_DEFAULT_AUDIO_FORMAT, format).apply();
    }

    public static String getDefaultAudioBitrate(Context context) {
        return getPrefs(context).getString(KEY_DEFAULT_AUDIO_BITRATE, "0");
    }

    public static void setDefaultAudioBitrate(Context context, String bitrate) {
        getPrefs(context).edit().putString(KEY_DEFAULT_AUDIO_BITRATE, bitrate).apply();
    }

    public static boolean isEmbedThumbnail(Context context) {
        return getPrefs(context).getBoolean(KEY_EMBED_THUMBNAIL, true);
    }

    public static void setEmbedThumbnail(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_EMBED_THUMBNAIL, value).apply();
    }

    public static boolean isEmbedMetadata(Context context) {
        return getPrefs(context).getBoolean(KEY_EMBED_METADATA, true);
    }

    public static void setEmbedMetadata(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_EMBED_METADATA, value).apply();
    }

    public static boolean isEmbedSubtitles(Context context) {
        return getPrefs(context).getBoolean(KEY_EMBED_SUBTITLES, false);
    }

    public static void setEmbedSubtitles(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_EMBED_SUBTITLES, value).apply();
    }

    public static boolean isSplitChapters(Context context) {
        return getPrefs(context).getBoolean(KEY_SPLIT_CHAPTERS, false);
    }

    public static void setSplitChapters(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_SPLIT_CHAPTERS, value).apply();
    }

    public static long getLastCoreCheckTimestamp(Context context) {
        return getPrefs(context).getLong("last_core_check_ts", 0L);
    }

    public static void setLastCoreCheckTimestamp(Context context, long value) {
        getPrefs(context).edit().putLong("last_core_check_ts", value).apply();
    }

    public static final String KEY_HOOK_YOUTUBE = "hook_youtube_native_download";
    public static final String KEY_HOOK_YTMUSIC = "hook_ytmusic_native_download";

    public static boolean isYouTubeHookEnabled(Context context) {
        if (context == null) return true;
        try {
            return tn.eluea.kgpt.provider.WorldReadablePrefs.getPrefs(context).getBoolean(KEY_HOOK_YOUTUBE, true);
        } catch (Throwable e) {
            return getPrefs(context).getBoolean(KEY_HOOK_YOUTUBE, true);
        }
    }

    public static void setYouTubeHookEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_HOOK_YOUTUBE, enabled).apply();
        try {
            tn.eluea.kgpt.provider.WorldReadablePrefs.getPrefs(context).edit().putBoolean(KEY_HOOK_YOUTUBE, enabled).commit();
        } catch (Throwable ignored) {}
    }

    public static boolean isYTMusicHookEnabled(Context context) {
        if (context == null) return true;
        try {
            return tn.eluea.kgpt.provider.WorldReadablePrefs.getPrefs(context).getBoolean(KEY_HOOK_YTMUSIC, true);
        } catch (Throwable e) {
            return getPrefs(context).getBoolean(KEY_HOOK_YTMUSIC, true);
        }
    }

    public static void setYTMusicHookEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_HOOK_YTMUSIC, enabled).apply();
        try {
            tn.eluea.kgpt.provider.WorldReadablePrefs.getPrefs(context).edit().putBoolean(KEY_HOOK_YTMUSIC, enabled).commit();
        } catch (Throwable ignored) {}
    }

    public static boolean isCoreInstalled(Context context) {
        if (context == null) return false;
        return DownloaderEngine.getInstance().isCoreInstalled(context);
    }

    public static void setCoreInstalled(Context context, boolean value) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean("core_installed", value).apply();
    }
}
