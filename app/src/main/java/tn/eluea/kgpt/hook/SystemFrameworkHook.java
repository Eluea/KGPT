/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.hook;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * System Framework Hook (system_server / "android" package).
 * 
 * Provides:
 *  1. Global MediaSession Monitoring (Captures what's playing in ANY app: YouTube, YouTube Music, Spotify, etc.)
 *  2. Global Clipboard Interception (Zero-delay URL detection bypassing Android 10+ background restrictions)
 *  3. Global Window Blur Enabling (CrossWindowBlurListeners)
 */
public class SystemFrameworkHook {

    private static final String TAG = "KGPT_SYS_HOOK";
    public static final String ACTION_SYSTEM_MEDIA_UPDATE = "tn.eluea.kgpt.ACTION_SYSTEM_MEDIA_UPDATE";
    public static final String ACTION_SYSTEM_CLIPBOARD_URL = "tn.eluea.kgpt.ACTION_SYSTEM_CLIPBOARD_URL";
    public static final String TARGET_PKG = "tn.eluea.kgpt";

    // Active media state across the whole OS
    private static volatile String lastMediaPackage = null;
    private static volatile String lastMediaTitle = null;
    private static volatile String lastMediaArtist = null;
    private static volatile String lastMediaUrl = null;
    private static volatile String lastMediaId = null;

    public static void hook(HookManager hookManager, ClassLoader classLoader) {
        log("Initializing System Framework Core Hooks in system_server...");

        hookWindowBlur(hookManager, classLoader);
        hookMediaSessionService(hookManager, classLoader);
        hookClipboardService(hookManager, classLoader);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    // ==================== 1. WINDOW BLUR (AOSP WindowManager) ====================

    private static void hookWindowBlur(HookManager hookManager, ClassLoader classLoader) {
        try {
            Class<?> blurListenersClass = classLoader.loadClass("com.android.server.wm.CrossWindowBlurListeners");
            Method m = blurListenersClass.getDeclaredMethod("isCrossWindowBlurEnabled");
            hookManager.hook(m, MethodHook.before(param -> param.setResult(true)));
            log("Hooked CrossWindowBlurListeners.isCrossWindowBlurEnabled -> true");
        } catch (Throwable t) {
            log("CrossWindowBlurListeners hook failed: " + t.getMessage());
        }

        try {
            Class<?> wmServiceClass = classLoader.loadClass("com.android.server.wm.WindowManagerService");
            Method m = wmServiceClass.getDeclaredMethod("isCrossWindowBlurEnabled");
            hookManager.hook(m, MethodHook.before(param -> param.setResult(true)));
            log("Hooked WindowManagerService.isCrossWindowBlurEnabled -> true");
        } catch (Throwable t) {
            log("WindowManagerService hook failed: " + t.getMessage());
        }

        try {
            Class<?> clientBlurClass = classLoader.loadClass("android.view.CrossWindowBlurListeners");
            Method m = clientBlurClass.getDeclaredMethod("isCrossWindowBlurEnabled");
            hookManager.hook(m, MethodHook.before(param -> param.setResult(true)));
            log("Hooked android.view.CrossWindowBlurListeners -> true");
        } catch (Throwable t) {
            log("android.view.CrossWindowBlurListeners hook failed: " + t.getMessage());
        }
    }

    // ==================== 2. GLOBAL MEDIASESSION (MediaSessionRecord) ====================

    private static void hookMediaSessionService(HookManager hookManager, ClassLoader classLoader) {
        try {
            Class<?> recordClass = classLoader.loadClass("com.android.server.media.MediaSessionRecord");

            // Hook setMetadata(MediaMetadata)
            for (Method m : recordClass.getDeclaredMethods()) {
                if (m.getName().equals("setMetadata") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == MediaMetadata.class) {
                    hookManager.hook(m, MethodHook.after(param -> {
                        Object record = param.getThisObject();
                        MediaMetadata metadata = (MediaMetadata) param.getArgs()[0];
                        if (record != null && metadata != null) {
                            handleSystemMediaMetadata(record, metadata);
                        }
                    }));
                    log("Hooked MediaSessionRecord.setMetadata successfully");
                } else if (m.getName().equals("setPlaybackState") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == PlaybackState.class) {
                    hookManager.hook(m, MethodHook.after(param -> {
                        Object record = param.getThisObject();
                        PlaybackState state = (PlaybackState) param.getArgs()[0];
                        if (record != null && state != null) {
                            handleSystemPlaybackState(record, state);
                        }
                    }));
                    log("Hooked MediaSessionRecord.setPlaybackState successfully");
                }
            }
        } catch (Throwable t) {
            log("Failed to hook MediaSessionRecord: " + t.getMessage());
        }
    }

    private static void handleSystemMediaMetadata(Object record, MediaMetadata metadata) {
        try {
            String pkg = extractPackageNameFromRecord(record);
            CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            CharSequence album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
            CharSequence mediaId = metadata.getText(MediaMetadata.METADATA_KEY_MEDIA_ID);
            CharSequence mediaUri = metadata.getText(MediaMetadata.METADATA_KEY_MEDIA_URI);

            log(">>> [GLOBAL MEDIA SESSION] App: " + pkg + " | Title: " + title + " | Artist: " + artist + " | MediaID: " + mediaId);

            lastMediaPackage = pkg;
            lastMediaTitle = title != null ? title.toString() : null;
            lastMediaArtist = artist != null ? artist.toString() : null;

            if (mediaId != null && isValidVideoId(mediaId.toString())) {
                lastMediaId = mediaId.toString();
                lastMediaUrl = "https://www.youtube.com/watch?v=" + lastMediaId;
            } else if (mediaUri != null && mediaUri.toString().contains("youtube.com")) {
                lastMediaUrl = mediaUri.toString();
            }

            MediaDescription desc = metadata.getDescription();
            if (desc != null) {
                if (desc.getMediaId() != null && isValidVideoId(desc.getMediaId())) {
                    lastMediaId = desc.getMediaId();
                    lastMediaUrl = "https://www.youtube.com/watch?v=" + lastMediaId;
                }
                if (desc.getMediaUri() != null) {
                    lastMediaUrl = desc.getMediaUri().toString();
                }
            }
        } catch (Throwable t) {
            log("Error handling system metadata: " + t.getMessage());
        }
    }

    private static void handleSystemPlaybackState(Object record, PlaybackState state) {
        try {
            String pkg = extractPackageNameFromRecord(record);
            if (state.getState() == PlaybackState.STATE_PLAYING) {
                log(">>> [GLOBAL MEDIA PLAYING] App: " + pkg);
            }
        } catch (Throwable ignored) {}
    }

    private static String extractPackageNameFromRecord(Object record) {
        if (record == null) return "unknown";
        try {
            Method getPkg = record.getClass().getMethod("getPackageName");
            Object res = getPkg.invoke(record);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        try {
            Field f = record.getClass().getDeclaredField("mPackageName");
            f.setAccessible(true);
            Object res = f.get(record);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        return "unknown";
    }

    // ==================== 3. GLOBAL CLIPBOARD (ClipboardService) ====================

    private static void hookClipboardService(HookManager hookManager, ClassLoader classLoader) {
        try {
            Class<?> clipServiceClass = classLoader.loadClass("com.android.server.clipboard.ClipboardService");

            for (Method m : clipServiceClass.getDeclaredMethods()) {
                String mName = m.getName();
                if (mName.startsWith("setPrimaryClip")) {
                    hookManager.hook(m, MethodHook.before(param -> {
                        Object[] args = param.getArgs();
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg instanceof ClipData) {
                                    ClipData cd = (ClipData) arg;
                                    handleGlobalClipData(cd);
                                    break;
                                }
                            }
                        }
                    }));
                    log("Hooked ClipboardService." + mName + " successfully");
                }
            }
        } catch (Throwable t) {
            log("Failed to hook ClipboardService: " + t.getMessage());
        }
    }

    private static void handleGlobalClipData(ClipData clip) {
        if (clip == null || clip.getItemCount() == 0) return;
        try {
            CharSequence text = clip.getItemAt(0).getText();
            if (text != null) {
                String s = text.toString().trim();
                log(">>> [GLOBAL SYSTEM CLIPBOARD] Copied: " + (s.length() > 60 ? s.substring(0, 60) + "..." : s));

                if (isMediaUrl(s)) {
                    log(">>> [GLOBAL MEDIA URL DETECTED] " + s);
                    lastMediaUrl = s;
                }
            }
        } catch (Throwable t) {
            log("Error reading global clip: " + t.getMessage());
        }
    }

    private static boolean isMediaUrl(String s) {
        if (TextUtils.isEmpty(s)) return false;
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("youtube.com") || l.contains("youtu.be") ||
               l.contains("tiktok.com") || l.contains("instagram.com") ||
               l.contains("facebook.com") || l.contains("fb.watch") ||
               l.contains("twitter.com") || l.contains("x.com") ||
               l.contains("soundcloud.com") || l.contains("spotify.com") ||
               l.contains("reddit.com") || l.contains("pinterest.com");
    }

    private static boolean isValidVideoId(String id) {
        if (id == null || id.length() != 11) return false;
        for (int i = 0; i < 11; i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_'))
                return false;
        }
        return true;
    }

    // ==================== GETTERS ====================

    public static String getLastMediaUrl() {
        return lastMediaUrl;
    }

    public static String getLastMediaTitle() {
        return lastMediaTitle;
    }

    public static String getLastMediaPackage() {
        return lastMediaPackage;
    }
}
