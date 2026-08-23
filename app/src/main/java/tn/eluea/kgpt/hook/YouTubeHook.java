/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.hook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import tn.eluea.kgpt.MainHook;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust YouTube & YouTube Music External Downloader Hook (STOCK & MODDED).
 *
 * [Layer 1] Locate videoId setters dynamically by stable DEX string fingerprint
 *           "Null currentVideoId" (or "Missing required properties:").
 *           Hooks every 1-String-parameter method on matched builder classes.
 * [Choke Point] MainOfflineVideoActionsController located dynamically by "MainOfflineVideoActionsController"
 *               Intercepts all download commands (start, format, manage) from any UI path.
 * [Layer 2] Hook framework android.view.View.setOnClickListener & OfflineArrowView.setOnClickListener.
 */
public class YouTubeHook {

    private static final String TAG = "YT_HOOK";
    public static final String TARGET_PKG = "tn.eluea.kgpt";
    public static final String TARGET_ACTIVITY = "tn.eluea.kgpt.features.downloader.ui.MediaShareActivity";

    private static volatile Context appContext = null;
    private static volatile String currentVideoId = "";
    private static final Set<String> hookedBuilderClasses = new HashSet<>();
    private static final Set<String> hookedControllerClasses = new HashSet<>();
    private static volatile boolean layer2Hooked = false;
    private static Class<?> offlineArrowClass = null;

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("(?<=^|[^a-zA-Z0-9_-])([a-zA-Z0-9_-]{11})(?=[^a-zA-Z0-9_-]|$)");

    private static volatile String currentPackageName = "";

    public static void hook(HookManager hookManager, ClassLoader classLoader, String packageName) {
        currentPackageName = packageName;
        log("Initializing Robust YouTube/YouTube Music Hook for " + packageName);

        // 1. Hook Framework View.setOnClickListener (Layer 2)
        hookLayer2DownloadClick(hookManager, classLoader);

        // 2. Hook Package Manager Visibility Bypass
        hookPackageManager(hookManager);

        // 3. Hook Application & Activity lifecycle to scan ClassLoader dynamically
        hookLifecycleAndScanner(hookManager, classLoader);

        // 4. Initial attempt on base ClassLoader
        scanAndHookDexKitLayers(hookManager, classLoader, null);
    }

    private static void log(String msg) {
        Log.i(TAG, msg);
        Log.d(TAG, msg);
    }

    // ============================================================
    // DexKit Native Library Dynamic Loader
    // ============================================================

    private static volatile boolean isDexKitLoaded = false;

    private static synchronized void ensureDexKitLoaded(Context context) {
        if (isDexKitLoaded) return;
        try {
            System.loadLibrary("dexkit");
            isDexKitLoaded = true;
            log("DexKit loaded via System.loadLibrary");
            return;
        } catch (Throwable ignored) {}

        try {
            ClassLoader cl = YouTubeHook.class.getClassLoader();
            if (cl instanceof dalvik.system.BaseDexClassLoader) {
                dalvik.system.BaseDexClassLoader bcl = (dalvik.system.BaseDexClassLoader) cl;
                String libPath = bcl.findLibrary("dexkit");
                if (libPath != null && new java.io.File(libPath).exists()) {
                    System.load(libPath);
                    isDexKitLoaded = true;
                    log("DexKit loaded via bcl.findLibrary: " + libPath);
                    return;
                }
            }
        } catch (Throwable t) {
            log("bcl.findLibrary error: " + t.getMessage());
        }

        // Search APK path from ClassLoader string or PackageManager
        try {
            String clStr = String.valueOf(YouTubeHook.class.getClassLoader());
            String apkPath = null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(/data/app/[^\":\\]]+\\.apk)").matcher(clStr);
            if (m.find()) {
                apkPath = m.group(1);
            }

            if (apkPath == null && context != null) {
                try {
                    ApplicationInfo ai = context.getPackageManager().getApplicationInfo(TARGET_PKG, 0);
                    if (ai != null) apkPath = ai.sourceDir;
                } catch (Throwable ignored) {}
            }

            if (apkPath != null && context != null) {
                java.io.File outSo = new java.io.File(context.getCodeCacheDir(), "libdexkit.so");
                String[] abis = android.os.Build.SUPPORTED_ABIS;
                try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(apkPath)) {
                    for (String abi : abis) {
                        java.util.zip.ZipEntry ze = zf.getEntry("lib/" + abi + "/libdexkit.so");
                        if (ze != null) {
                            try (java.io.InputStream is = zf.getInputStream(ze);
                                 java.io.FileOutputStream fos = new java.io.FileOutputStream(outSo)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                            }
                            System.load(outSo.getAbsolutePath());
                            isDexKitLoaded = true;
                            log("DexKit extracted from " + apkPath + " (" + abi + ") and loaded: " + outSo.getAbsolutePath());
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log("DexKit APK extract error: " + t.getMessage());
        }
    }

    // ============================================================
    // LAYER 1 & 3 & 4 : DexKit dynamic scanners
    // ============================================================

    private static synchronized void scanAndHookDexKitLayers(HookManager hookManager, ClassLoader classLoader, Context context) {
        if (classLoader == null) return;
        if (context != null) appContext = context;

        ensureDexKitLoaded(context);

        if (isDexKitLoaded) {
            try (DexKitBridge bridge = DexKitBridge.create(classLoader, false)) {

                // ------------------------------------------------------------
                // LAYER 1 : capture currentVideoId (observation, safe)
                // ------------------------------------------------------------
                try {
                    List<ClassData> builders = bridge.findClass(FindClass.create().matcher(
                            ClassMatcher.create().usingStrings(new String[]{"Null currentVideoId"})
                    ));

                    if (builders == null || builders.isEmpty()) {
                        builders = bridge.findClass(FindClass.create().matcher(
                                ClassMatcher.create().usingStrings(new String[]{"Missing required properties:"})
                        ));
                    }

                    if (builders != null && !builders.isEmpty()) {
                        int count = 0;
                        for (ClassData cd : builders) {
                            try {
                                Class<?> cls = cd.getInstance(classLoader);
                                if (hookSingleBuilderClass(hookManager, cls)) {
                                    count++;
                                }
                            } catch (Throwable ignored) {}
                        }
                        log("LAYER1 DexKit hooked: " + count + " builder class(es)");
                    }
                } catch (Throwable e) {
                    log("LAYER1 builder scan error: " + e.getMessage());
                }

                // ------------------------------------------------------------
                // LAYER 3 : terminal action interception (ALL paths incl Litho)
                // MainOfflineVideoActionsController - every download command ends here
                // ------------------------------------------------------------
                try {
                    List<ClassData> controllers = bridge.findClass(FindClass.create().matcher(
                            ClassMatcher.create().usingStrings(new String[]{"MainOfflineVideoActionsController"})
                    ));

                    if (controllers == null || controllers.isEmpty()) {
                        controllers = bridge.findClass(FindClass.create().matcher(
                                ClassMatcher.create().usingStrings(new String[]{"OfflineVideoActionsController"})
                        ));
                    }

                    if (controllers != null && !controllers.isEmpty()) {
                        int count = 0;
                        for (ClassData cd : controllers) {
                            try {
                                Class<?> cls = cd.getInstance(classLoader);
                                if (hookLayer3ControllerClass(hookManager, cls)) {
                                    count++;
                                }
                            } catch (Throwable ignored) {}
                        }
                        log("LAYER3 hooked: " + count + " MainOfflineVideoActionsController class(es)");
                    }
                } catch (Throwable e) {
                    log("LAYER3 controller scan error: " + e.getMessage());
                }

                // ------------------------------------------------------------
                // LAYER 2b : Hook pub (OnClickListener classes associated with OfflineArrowView)
                // ------------------------------------------------------------
                try {
                    List<ClassData> arrowListeners = bridge.findClass(FindClass.create().matcher(
                            ClassMatcher.create().usingStrings(new String[]{"OfflineArrowView"})
                    ));
                    if (arrowListeners != null) {
                        for (ClassData cd : arrowListeners) {
                            try {
                                Class<?> cls = cd.getInstance(classLoader);
                                if (View.OnClickListener.class.isAssignableFrom(cls)) {
                                    for (Method m : cls.getDeclaredMethods()) {
                                        if ("onClick".equals(m.getName()) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == View.class) {
                                            hookManager.hook(m, MethodHook.before(p -> {
                                                View cv = (View) p.getArgs()[0];
                                                log("PUB ONCLICK INTERCEPTED: " + cls.getName() + " videoId=" + currentVideoId);
                                                Context c = cv != null ? cv.getContext() : appContext;
                                                openExternalDownloader(c, currentVideoId);
                                                p.setResult(null); // Consume click to suppress YouTube bottom sheet
                                            }));
                                            log("Hooked pub OnClickListener: " + cls.getName());
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable e) {
                    log("arrowListeners search error: " + e.getMessage());
                }

            } catch (Throwable e) {
                log("DexKitBridge scan error: " + e.getMessage());
            }
        }

        // Additional safety fallback for common un-obfuscated protobuf/descriptor classes
        fallbackHookKnownBuilders(hookManager, classLoader);

        // LAYER 4 : Elements CommandHandlerResolver observer
        hookLayer4ElementsResolver(hookManager, classLoader);
    }

    private static final Set<String> hookedCommandHandlerClasses = new HashSet<>();

    private static boolean hookCommandHandlerClass(HookManager hookManager, Class<?> cls, String fingerprint) {
        if (cls == null) return false;
        String name = cls.getName();
        if (name.contains("OuterClass") || name.endsWith("Endpoint") || name.contains("Builder")) return false;
        if (hookedCommandHandlerClasses.contains(name)) return false;
        hookedCommandHandlerClasses.add(name);

        int count = 0;
        for (Method m : cls.getDeclaredMethods()) {
            Class<?>[] pts = m.getParameterTypes();
            if (pts != null && pts.length >= 1) {
                try {
                    hookManager.hook(m, MethodHook.before(param -> {
                        log("LAYER5: InnerTube CommandHandler intercepted on " + name + "." + m.getName() + " (fp=" + fingerprint + ") videoId=" + currentVideoId);
                        if (!TextUtils.isEmpty(currentVideoId)) {
                            openExternalDownloader(appContext, currentVideoId);
                            if (m.getReturnType() == void.class) {
                                param.setResult(null);
                            } else if (m.getReturnType() == boolean.class) {
                                param.setResult(true);
                            }
                        }
                    }));
                    count++;
                } catch (Throwable ignored) {}
            }
        }
        return count > 0;
    }

    private static boolean hookLayer3ControllerClass(HookManager hookManager, Class<?> cls) {
        if (cls == null) return false;
        String name = cls.getName();
        if (hookedControllerClasses.contains(name)) return false;
        hookedControllerClasses.add(name);

        int hookedCount = 0;
        for (Method m : cls.getDeclaredMethods()) {
            try {
                hookManager.hook(m, MethodHook.before(param -> {
                    Object[] args = param.getArgs();
                    String foundId = null;

                    if (args != null && args.length > 0) {
                        for (Object arg : args) {
                            if (arg instanceof String) {
                                String clean = extractCleanVideoId((String) arg);
                                if (clean != null) {
                                    foundId = clean;
                                    break;
                                }
                            }
                        }
                    }

                    if (foundId == null && !TextUtils.isEmpty(currentVideoId)) {
                        foundId = currentVideoId;
                    }

                    if (foundId != null) {
                        currentVideoId = foundId;
                        log("DOWNLOAD_ACTION (LAYER3 terminal) id=" + currentVideoId + " method=" + m.getName() + " on " + name);

                        Context ctx = getValidContext(param.getThisObject());
                        if (ctx == null) ctx = appContext;
                        openExternalDownloader(ctx, currentVideoId);

                        // Block YouTube's internal download / quality dialog / upsell
                        if (m.getReturnType() == void.class) {
                            param.setResult(null);
                        } else if (m.getReturnType() == boolean.class) {
                            param.setResult(true);
                        }
                    }
                }));
                hookedCount++;
            } catch (Throwable ignored) {}
        }
        log("LAYER3 hooked " + hookedCount + " methods on " + name);
        return hookedCount > 0;
    }

    // ------------------------------------------------------------
    // LAYER 4 : Elements CommandHandlerResolver (JNI-stable Litho Command Dispatcher)
    // ------------------------------------------------------------

    private static void hookLayer4ElementsResolver(HookManager hookManager, ClassLoader classLoader) {
        try {
            Class<?> resolverCls = Class.forName("com.google.android.libraries.elements.interfaces.CommandHandlerResolver", false, classLoader);
            for (Method m : resolverCls.getDeclaredMethods()) {
                if ("c".equals(m.getName()) || "handleCommand".equals(m.getName()) || m.getParameterTypes().length == 1) {
                    hookManager.hook(m, MethodHook.before(param -> {
                        Object[] args = param.getArgs();
                        if (args != null && args.length > 0 && args[0] != null) {
                            String cmdStr = args[0].toString().toLowerCase(java.util.Locale.ROOT);
                            if (cmdStr.contains("offline") || cmdStr.contains("download")) {
                                log("L4_CMD(offline): " + (cmdStr.length() > 200 ? cmdStr.substring(0, 200) : cmdStr));
                                if (!TextUtils.isEmpty(currentVideoId)) {
                                    openExternalDownloader(appContext, currentVideoId);
                                }
                            }
                        }
                    }));
                    log("LAYER4 hooked: CommandHandlerResolver." + m.getName());
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Context getValidContext(Object thisObj) {
        if (thisObj == null) return null;
        try {
            try {
                Method m = thisObj.getClass().getMethod("getContext");
                Object res = m.invoke(thisObj);
                if (res instanceof Context) return (Context) res;
            } catch (Throwable ignored) {}

            for (Field f : thisObj.getClass().getDeclaredFields()) {
                if (Context.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(thisObj);
                    if (val instanceof Context) return (Context) val;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void fallbackHookKnownBuilders(HookManager hookManager, ClassLoader classLoader) {
        if (classLoader == null) return;
        String[] known = {
                "com.google.android.libraries.youtube.player.model.PlaybackStartDescriptor$Builder",
                "com.google.android.libraries.youtube.player.model.PlaybackStartDescriptor",
                "com.google.protos.youtube.api.innertube.WatchEndpointOuterClass$WatchEndpoint$Builder",
                "com.google.protos.youtube.api.innertube.WatchEndpointOuterClass$WatchEndpoint",
                "com.google.protos.youtube.api.innertube.ReelWatchEndpointOuterClass$ReelWatchEndpoint$Builder",
                "com.google.protos.youtube.api.innertube.ReelWatchEndpointOuterClass$ReelWatchEndpoint",
                "com.google.protos.youtube.api.innertube.OfflineVideoEndpointOuterClass$OfflineVideoEndpoint$Builder",
                "com.google.protos.youtube.api.innertube.OfflineVideoEndpointOuterClass$OfflineVideoEndpoint",
                "com.google.protos.youtube.api.innertube.MusicPlaybackEndpointOuterClass$MusicPlaybackEndpoint",
                "com.google.protos.youtube.api.innertube.MusicQueueEndpointOuterClass$MusicQueueEndpoint"
        };
        for (String name : known) {
            try {
                Class<?> cls = classLoader.loadClass(name);
                hookSingleBuilderClass(hookManager, cls);
            } catch (Throwable ignored) {}
        }
    }

    private static boolean hookSingleBuilderClass(HookManager hookManager, Class<?> cls) {
        if (cls == null) return false;
        String name = cls.getName();
        if (hookedBuilderClasses.contains(name)) return false;
        hookedBuilderClasses.add(name);

        boolean anyHooked = false;
        for (Method m : cls.getDeclaredMethods()) {
            Class<?>[] pts = m.getParameterTypes();
            // 1. Setter hook (1 String parameter)
            if (pts.length == 1 && pts[0] == String.class) {
                try {
                    hookManager.hook(m, MethodHook.before(param -> {
                        Object[] args = param.getArgs();
                        if (args != null && args.length > 0 && args[0] != null) {
                            String raw = args[0].toString().trim();
                            if (!TextUtils.isEmpty(raw)) {
                                String id = extractCleanVideoId(raw);
                                if (id != null) {
                                    currentVideoId = id;
                                    log("VIDEO_CHANGED (setter " + m.getName() + ") id=" + currentVideoId + " url=https://youtu.be/" + currentVideoId);
                                }
                            }
                        }
                    }));
                    anyHooked = true;
                } catch (Throwable ignored) {}
            }
            // 2. Getter hook (0 parameters, returns String)
            else if (pts.length == 0 && m.getReturnType() == String.class) {
                try {
                    hookManager.hook(m, MethodHook.after(param -> {
                        Object res = param.getResult();
                        if (res instanceof String) {
                            String raw = ((String) res).trim();
                            if (!TextUtils.isEmpty(raw)) {
                                String id = extractCleanVideoId(raw);
                                if (id != null) {
                                    currentVideoId = id;
                                    log("VIDEO_CHANGED (getter " + m.getName() + ") id=" + currentVideoId + " url=https://youtu.be/" + currentVideoId);
                                }
                            }
                        }
                    }));
                    anyHooked = true;
                } catch (Throwable ignored) {}
            }
        }
        return anyHooked;
    }

    private static String extractCleanVideoId(String text) {
        if (text == null || text.isEmpty()) return null;

        // If direct 11-char ID
        if (text.length() == 11 && isValidVideoId(text)) {
            return text;
        }

        // If URL
        if (text.contains("youtu.be/")) {
            int idx = text.indexOf("youtu.be/");
            String sub = text.substring(idx + "youtu.be/".length());
            int end = sub.indexOf('?');
            if (end != -1) sub = sub.substring(0, end);
            end = sub.indexOf('&');
            if (end != -1) sub = sub.substring(0, end);
            if (isValidVideoId(sub)) return sub;
        }

        if (text.contains("v=")) {
            int idx = text.indexOf("v=");
            String sub = text.substring(idx + 2);
            int end = sub.indexOf('&');
            if (end != -1) sub = sub.substring(0, end);
            if (isValidVideoId(sub)) return sub;
        }

        // Regex fallback
        Matcher m = VIDEO_ID_PATTERN.matcher(text);
        if (m.find()) {
            String candidate = m.group(1);
            if (isValidVideoId(candidate)) return candidate;
        }

        return null;
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

    // ============================================================
    // LAYER 2 : Intercept real View click (arrow, dialog options, performClick)
    // ============================================================

    private static Class<?> offlineOptionClass = null;

    private static void hookLayer2DownloadClick(HookManager hookManager, ClassLoader classLoader) {
        if (layer2Hooked) return;

        try {
            try {
                offlineArrowClass = Class.forName("com.google.android.apps.youtube.app.offline.ui.OfflineArrowView", false, classLoader);
                log("OfflineArrowView resolved: " + offlineArrowClass.getName());
            } catch (Throwable ignored) {}

            try {
                offlineOptionClass = Class.forName("com.google.android.libraries.youtube.offline.ui.OfflineDialogOptionView", false, classLoader);
                log("OfflineDialogOptionView resolved: " + offlineOptionClass.getName());
            } catch (Throwable ignored) {}

            // Hook View.setContentDescription to tag any download views
            hookManager.hook(View.class, "setContentDescription", new Class<?>[]{CharSequence.class}, MethodHook.before(param -> {
                Object obj = param.getThisObject();
                if (obj instanceof View && param.getArgs() != null && param.getArgs().length > 0 && param.getArgs()[0] != null) {
                    CharSequence cd = (CharSequence) param.getArgs()[0];
                    if (isDownloadKeyword(cd.toString())) {
                        ((View) obj).setTag(android.R.id.custom, Boolean.TRUE);
                    }
                }
            }));

            // Hook View.performClick()
            hookManager.hook(View.class, "performClick", new Class<?>[]{}, MethodHook.before(param -> {
                Object obj = param.getThisObject();
                if (!(obj instanceof View)) return;
                View v = (View) obj;

                if (isDownloadButtonView(v)) {
                    log("LAYER2: performClick intercepted on " + v.getClass().getSimpleName() + ", videoId=" + currentVideoId);
                    openExternalDownloader(v.getContext(), currentVideoId);
                    param.setResult(true); // Consume click
                }
            }));

            // Zero-Flicker Dialog Hook: Cancel in BEFORE if possible, or hide window and dismiss in AFTER
            hookManager.hook(android.app.Dialog.class, "show", new Class<?>[]{}, MethodHook.before(param -> {
                android.app.Dialog dialog = (android.app.Dialog) param.getThisObject();
                if (dialog == null) return;

                if (isDownloadOrUpsellDialog(dialog)) {
                    log("DIALOG CANCELLED IN BEFORE (0ms zero-flicker): " + dialog.getClass().getName());
                    openExternalDownloader(dialog.getContext(), currentVideoId);
                    param.setResult(null);
                    return;
                }

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setWindowAnimations(0);
                }
            }));

            hookManager.hook(android.app.Dialog.class, "show", new Class<?>[]{}, MethodHook.after(param -> {
                android.app.Dialog dialog = (android.app.Dialog) param.getThisObject();
                if (dialog == null) return;
                String dName = dialog.getClass().getName();

                if (isDownloadOrUpsellDialog(dialog)) {
                    log("DOWNLOAD QUALITY/UPSELL DIALOG CONFIRMED (sync after): Dismissing and opening KGPT: " + dName);
                    dialog.dismiss();
                    openExternalDownloader(dialog.getContext(), currentVideoId);
                    return;
                }

                if (dialog.getWindow() != null && dialog.getWindow().getDecorView() != null) {
                    View decor = dialog.getWindow().getDecorView();
                    decor.post(() -> {
                        if (isDownloadOrUpsellDialog(dialog)) {
                            log("DOWNLOAD QUALITY/UPSELL DIALOG CONFIRMED (post): Dismissing and opening KGPT: " + dName);
                            dialog.dismiss();
                            openExternalDownloader(dialog.getContext(), currentVideoId);
                        }
                    });
                }
            }));

            // Track active Activity and suppress PiP mode
            hookManager.hook(Activity.class, "onResume", new Class<?>[]{}, MethodHook.after(param -> {
                Activity act = (Activity) param.getThisObject();
                if (act != null) {
                    activeActivityRef = new java.lang.ref.WeakReference<>(act);
                }
            }));

            hookManager.hook(Activity.class, "enterPictureInPictureMode", new Class<?>[]{}, MethodHook.before(param -> {
                if (isOpeningDownloader) {
                    log("PREVENTED YouTube enterPictureInPictureMode during download sheet launch");
                    param.setResult(false);
                }
            }));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                hookManager.hook(Activity.class, "enterPictureInPictureMode", new Class<?>[]{android.app.PictureInPictureParams.class}, MethodHook.before(param -> {
                    if (isOpeningDownloader) {
                        log("PREVENTED YouTube enterPictureInPictureMode(params) during download sheet launch");
                        param.setResult(false);
                    }
                }));
            }

            // Hook Activity.dispatchTouchEvent
            hookManager.hook(Activity.class, "dispatchTouchEvent", new Class<?>[]{android.view.MotionEvent.class}, MethodHook.before(param -> {
                Activity act = (Activity) param.getThisObject();
                android.view.MotionEvent ev = (android.view.MotionEvent) param.getArgs()[0];
                if (act == null || ev == null) return;

                if (ev.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                    float x = ev.getRawX();
                    float y = ev.getRawY();
                    if (act.getWindow() != null && act.getWindow().getDecorView() != null) {
                        View hit = findHitView(act.getWindow().getDecorView(), (int) x, (int) y);
                        if (hit != null) {
                            captureTrackTitleFromView(hit);
                            if (isDownloadButtonView(hit)) {
                                log("LAYER2: Touch UP intercepted on download view: " + hit.getClass().getSimpleName() + " videoId=" + currentVideoId);
                                openExternalDownloader(act, currentVideoId);
                            }
                        }
                    }
                }
            }));

            layer2Hooked = true;
            log("LAYER2 hooked: performClick, setContentDescription, Dialog.show, and Touch registered");
        } catch (Throwable e) {
            log("LAYER2 error: " + e.getMessage());
        }
    }

    private static boolean isDownloadButtonView(View view) {
        if (view == null) return false;

        // 1. Tagged by setContentDescription
        if (Boolean.TRUE.equals(view.getTag(android.R.id.custom))) return true;

        // 2. Check current view
        if (matchesDownload(view)) return true;

        // 3. Check parents up to 4 levels
        android.view.ViewParent p = view.getParent();
        int depth = 0;
        while (p instanceof View && depth < 4) {
            if (matchesDownload((View) p)) return true;
            p = p.getParent();
            depth++;
        }

        // 4. Check children recursively
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = vg.getChildAt(i);
                if (matchesDownload(child)) return true;
            }
        }

        return false;
    }

    private static boolean matchesDownload(View view) {
        if (view == null) return false;
        if (offlineArrowClass != null && offlineArrowClass.isInstance(view)) return true;
        if (offlineOptionClass != null && offlineOptionClass.isInstance(view)) return true;

        if (view.getId() != View.NO_ID && view.getResources() != null) {
            try {
                String entry = view.getResources().getResourceEntryName(view.getId()).toLowerCase(java.util.Locale.ROOT);
                if (entry.contains("download") || entry.contains("offline")) return true;
            } catch (Throwable ignored) {}
        }

        CharSequence cd = view.getContentDescription();
        if (cd != null && isDownloadKeyword(cd.toString())) return true;

        if (view instanceof android.widget.TextView) {
            CharSequence txt = ((android.widget.TextView) view).getText();
            if (txt != null && isDownloadKeyword(txt.toString())) return true;
        }

        Object tag = view.getTag();
        if (tag instanceof String && isDownloadKeyword((String) tag)) return true;

        return false;
    }

    private static boolean isDownloadKeyword(String raw) {
        if (raw == null) return false;
        String t = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return t.equals("download") || t.equals("تنزيل") || t.equals("تحميل") ||
                t.contains("download video") || t.contains("download song") ||
                t.contains("تنزيل الفيديو") || t.contains("تحميل الفيديو") ||
                t.contains("تنزيل الأغنية") || t.contains("تحميل الأغنية") ||
                t.contains("offline") || t.contains("telecharger") || t.contains("descargar");
    }

    private static boolean isDownloadOrUpsellDialog(android.app.Dialog dialog) {
        if (dialog == null) return false;
        android.view.Window w = dialog.getWindow();
        if (w == null || w.getDecorView() == null) return false;

        java.util.List<String> allTexts = new java.util.ArrayList<>();
        collectAllTexts(w.getDecorView(), allTexts);

        // 1. If it contains multi-option 3-dots / settings / playlist menu items, IGNORE IT!
        for (String t : allTexts) {
            if (t.contains("playback speed") || t.contains("سرعة التشغيل") ||
                t.contains("captions") || t.contains("الترجمة") ||
                t.contains("loop video") || t.contains("تكرار الفيديو") ||
                t.contains("lock screen") || t.contains("قفل الشاشة") ||
                t.contains("report") || t.contains("إبلاغ") ||
                t.contains("ambient mode") || t.contains("إضاءة سينمائية") ||
                t.contains("listening controls") || t.contains("عناصر التحكم في الاستماع") ||
                t.contains("additional settings") || t.contains("إعدادات إضافية") ||
                t.contains("stats for nerds") || t.contains("إحصاءات مفصّلة") ||
                t.contains("sleep timer") || t.contains("مؤقت النوم") ||
                t.contains("start radio") || t.contains("بدء الراديو") ||
                t.contains("play next") || t.contains("تشغيل التالي") ||
                t.contains("add to queue") || t.contains("إضافة إلى قائمة الانتظار") ||
                t.contains("go to artist") || t.contains("الانتقال إلى فنان")) {
                return false;
            }
        }

        // 2. Direct match for YouTube Music & YouTube Premium Upsell Dialog
        for (String t : allTexts) {
            if (t.contains("get music premium") || t.contains("احصل على music premium") ||
                t.contains("listen to songs offline") || t.contains("الاستماع إلى الأغاني بلا اتصال") ||
                t.contains("download your favorite songs") || t.contains("تنزيل أغانيك المفضلة") ||
                t.contains("get ad-free music") || t.contains("موسيقى بلا إعلانات") ||
                t.contains("play music in the background") || t.contains("تشغيل الموسيقى في الخلفية") ||
                t.contains("cancel anytime") || t.contains("إلغاء في أي وقت")) {
                return true;
            }
        }

        // 3. General download & quality checks
        boolean hasDownloadWord = false;
        boolean hasQuality = false;
        boolean hasUpsellOrPremium = false;

        for (String t : allTexts) {
            if (t.contains("download") || t.contains("تنزيل") || t.contains("تحميل") ||
                t.contains("offline") || t.contains("بلا إنترنت") || t.contains("بدون إنترنت") ||
                t.contains("telecharger") || t.contains("descargar")) {
                hasDownloadWord = true;
            }
            if (t.contains("720p") || t.contains("1080p") || t.contains("480p") || t.contains("360p") || t.contains("144p") ||
                t.contains("متوسطة") || t.contains("عالية") || t.contains("منخفضة") ||
                t.contains("medium") || t.contains("high") || t.contains("low") ||
                t.contains("جودة التنزيل") || t.contains("download quality")) {
                hasQuality = true;
            }
            if (t.contains("premium") || t.contains("بريميوم") ||
                t.contains("music premium") || t.contains("youtube premium") ||
                t.contains("تجربة مجانية") || t.contains("free trial") ||
                t.contains("اشتراك") || t.contains("subscribe") ||
                t.contains("احصل على") || t.contains("get premium")) {
                hasUpsellOrPremium = true;
            }
        }

        // Match: (Download + Quality) OR (Download + Upsell/Premium) OR Small Download Dialog
        return (hasDownloadWord && hasQuality) ||
               (hasDownloadWord && hasUpsellOrPremium) ||
               (hasUpsellOrPremium && allTexts.size() <= 16) ||
               (hasDownloadWord && allTexts.size() <= 8);
    }

    private static void collectAllTexts(View v, java.util.List<String> out) {
        if (v == null) return;
        if (v instanceof android.widget.TextView) {
            CharSequence txt = ((android.widget.TextView) v).getText();
            if (txt != null && txt.length() > 0) {
                out.add(txt.toString().toLowerCase(java.util.Locale.ROOT));
            }
        }
        if (v.getContentDescription() != null) {
            out.add(v.getContentDescription().toString().toLowerCase(java.util.Locale.ROOT));
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectAllTexts(vg.getChildAt(i), out);
            }
        }
    }

    private static View findHitView(View root, int rawX, int rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) return null;
        int[] loc = new int[2];
        root.getLocationOnScreen(loc);
        if (rawX < loc[0] || rawX > loc[0] + root.getWidth() ||
            rawY < loc[1] || rawY > loc[1] + root.getHeight()) return null;

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                View hit = findHitView(vg.getChildAt(i), rawX, rawY);
                if (hit != null) return hit;
            }
        }
        return root;
    }

    private static void dumpViewHierarchy(View v, int depth) {
        if (v == null || depth > 10) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(v.getClass().getSimpleName());
        if (v instanceof android.widget.TextView) {
            sb.append(" [text='").append(((android.widget.TextView) v).getText()).append("']");
        }
        if (v.getContentDescription() != null) {
            sb.append(" [cd='").append(v.getContentDescription()).append("']");
        }
        log("VIEW_TREE: " + sb.toString());
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpViewHierarchy(vg.getChildAt(i), depth + 1);
            }
        }
    }

    private static void captureTrackTitleFromView(View v) {
        if (v == null) return;
        View parent = v;
        for (int i = 0; i < 4 && parent.getParent() instanceof View; i++) {
            parent = (View) parent.getParent();
            java.util.List<String> texts = new java.util.ArrayList<>();
            collectRowTextViews(parent, texts);
            if (texts.size() >= 1 && texts.size() <= 4) {
                StringBuilder sb = new StringBuilder();
                for (String t : texts) {
                    if (!t.contains("views") && !t.contains("plays") && !t.contains("مشاهدة") && !t.contains("استماع")) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(t.replaceAll("[•·].*", "").trim());
                    }
                }
                String candidate = sb.toString().trim();
                if (candidate.length() > 2 && candidate.length() < 100) {
                    lastSelectedTrackTitle = candidate;
                    log("CAPTURED_TRACK_TITLE: " + lastSelectedTrackTitle);
                    break;
                }
            }
        }
    }

    private static void collectRowTextViews(View v, java.util.List<String> out) {
        if (v == null) return;
        if (v instanceof android.widget.TextView) {
            CharSequence txt = ((android.widget.TextView) v).getText();
            if (txt != null && txt.length() > 0) {
                out.add(txt.toString().trim());
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectRowTextViews(vg.getChildAt(i), out);
            }
        }
    }

    // ============================================================
    // External Downloader Launcher
    // ============================================================

    private static volatile String lastOpenedVideo = "";
    private static volatile long lastOpenedTime = 0;
    private static volatile String lastSelectedTrackTitle = "";
    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static java.lang.ref.WeakReference<Activity> activeActivityRef = new java.lang.ref.WeakReference<>(null);
    private static volatile boolean isOpeningDownloader = false;

    private static void openExternalDownloader(Context context, String videoIdOrQuery) {
        if (videoIdOrQuery == null || videoIdOrQuery.isEmpty()) {
            if (!TextUtils.isEmpty(lastSelectedTrackTitle)) {
                videoIdOrQuery = lastSelectedTrackTitle;
            } else {
                log("no videoId or track title yet, cannot trigger download");
                return;
            }
        }

        long now = System.currentTimeMillis();
        // dedupe: avoid reopening the same video twice within 2s
        if (videoIdOrQuery.equals(lastOpenedVideo) && (now - lastOpenedTime) < 2000) return;
        lastOpenedVideo = videoIdOrQuery;
        lastOpenedTime = now;

        final Context targetContext = (context != null) ? context : appContext;
        if (targetContext == null) return;

        if ("com.google.android.youtube".equals(currentPackageName) && !tn.eluea.kgpt.features.downloader.core.DownloaderPrefs.isYouTubeHookEnabled(targetContext)) {
            log("Download bypassed: YouTube native hook is disabled in settings");
            return;
        }
        if ("com.google.android.apps.youtube.music".equals(currentPackageName) && !tn.eluea.kgpt.features.downloader.core.DownloaderPrefs.isYTMusicHookEnabled(targetContext)) {
            log("Download bypassed: YouTube Music native hook is disabled in settings");
            return;
        }

        isOpeningDownloader = true;
        mainHandler.postDelayed(() -> isOpeningDownloader = false, 3000);

        Activity currentAct = activeActivityRef.get();
        if (currentAct == null && targetContext instanceof Activity) {
            currentAct = (Activity) targetContext;
        }

        if (currentAct != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                // Disable auto-enter PiP on Android 12+ so YouTube doesn't shrink into a floating box
                currentAct.setPictureInPictureParams(new android.app.PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build());
            } catch (Throwable ignored) {}
        }

        final String finalTarget = videoIdOrQuery;
        mainHandler.post(() -> {
            String url = finalTarget.startsWith("http")
                    ? finalTarget
                    : (isValidVideoId(finalTarget) ? "https://youtu.be/" + finalTarget : finalTarget);
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setComponent(new ComponentName(TARGET_PKG, TARGET_ACTIVITY));
                intent.putExtra(Intent.EXTRA_TEXT, url);
                intent.putExtra("direct_download", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);

                targetContext.startActivity(intent);
                log("OPENED_DOWNLOADER target=" + url);
            } catch (Throwable err) {
                log("launch with component failed, trying generic ACTION_SEND: " + err.getMessage());
                try {
                    Intent genericIntent = new Intent(Intent.ACTION_SEND);
                    genericIntent.setType("text/plain");
                    genericIntent.setPackage(TARGET_PKG);
                    genericIntent.putExtra(Intent.EXTRA_TEXT, url);
                    genericIntent.putExtra("direct_download", true);
                    genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    targetContext.startActivity(genericIntent);
                    log("OPENED_DOWNLOADER via generic package intent target=" + url);
                } catch (Throwable err2) {
                    log("launch failed completely: " + err2.getMessage());
                }
            }
        });
    }

    // ============================================================
    // Application & Activity Lifecycle Scanner Hooks
    // ============================================================

    private static void hookLifecycleAndScanner(HookManager hookManager, ClassLoader classLoader) {
        try {
            hookManager.hook(
                    Application.class, "onCreate", new Class<?>[]{},
                    MethodHook.after(param -> {
                        Application app = (Application) param.getThisObject();
                        if (app != null) {
                            ClassLoader appCl = app.getClassLoader();
                            hookLayer2DownloadClick(hookManager, appCl);
                            scanAndHookDexKitLayers(hookManager, appCl, app.getApplicationContext());
                        }
                    })
            );
        } catch (Throwable ignored) {}

        try {
            hookManager.hook(
                    Activity.class, "onCreate", new Class<?>[]{Bundle.class},
                    MethodHook.after(param -> {
                        Activity activity = (Activity) param.getThisObject();
                        if (activity != null) {
                            ClassLoader actCl = activity.getClassLoader();
                            hookLayer2DownloadClick(hookManager, actCl);
                            scanAndHookDexKitLayers(hookManager, actCl, activity);
                        }
                    })
            );
        } catch (Throwable ignored) {}
    }

    // ============================================================
    // Package Manager Visibility Bypass
    // ============================================================

    private static void hookPackageManager(HookManager hookManager) {
        try {
            Class<?> apm = Class.forName("android.app.ApplicationPackageManager");

            hookManager.hook(apm, "getPackageInfo", new Class<?>[]{String.class, int.class},
                    MethodHook.before(param -> {
                        if (TARGET_PKG.equals(param.getArgs()[0])) {
                            param.setResult(makePkgInfo());
                        }
                    })
            );

            hookManager.hook(apm, "resolveActivity", new Class<?>[]{Intent.class, int.class},
                    MethodHook.before(param -> {
                        Intent i = (Intent) param.getArgs()[0];
                        if (i != null && isKgptIntent(i)) {
                            param.setResult(makeResolveInfo());
                        }
                    })
            );
        } catch (Throwable ignored) {}
    }

    private static boolean isKgptIntent(Intent i) {
        if (i == null) return false;
        if (TARGET_PKG.equals(i.getPackage())) return true;
        return i.getComponent() != null && TARGET_PKG.equals(i.getComponent().getPackageName());
    }

    @SuppressLint("WrongConstant")
    private static PackageInfo makePkgInfo() {
        PackageInfo pi = new PackageInfo();
        pi.packageName = TARGET_PKG;
        pi.versionCode = 12;
        pi.versionName = "4.1.2";
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = TARGET_PKG;
        ai.enabled = true;
        ai.flags = ApplicationInfo.FLAG_INSTALLED | ApplicationInfo.FLAG_ALLOW_BACKUP;
        pi.applicationInfo = ai;
        ActivityInfo act = new ActivityInfo();
        act.packageName = TARGET_PKG;
        act.name = TARGET_ACTIVITY;
        act.exported = true;
        act.enabled = true;
        act.applicationInfo = ai;
        pi.activities = new ActivityInfo[]{act};
        return pi;
    }

    private static ResolveInfo makeResolveInfo() {
        ResolveInfo ri = new ResolveInfo();
        ActivityInfo act = new ActivityInfo();
        act.packageName = TARGET_PKG;
        act.name = TARGET_ACTIVITY;
        act.exported = true;
        act.enabled = true;
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = TARGET_PKG;
        ai.enabled = true;
        act.applicationInfo = ai;
        ri.activityInfo = act;
        return ri;
    }
}
