/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;

import java.lang.reflect.Member;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

import tn.eluea.kgpt.hook.HookManager;
import tn.eluea.kgpt.hook.MethodHook;
import tn.eluea.kgpt.hook.TextSelectionHook;
import tn.eluea.kgpt.hook.YouTubeHook;
import tn.eluea.kgpt.provider.XposedConfigReader;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;

public class MainHook extends XposedModule {
    private static MainHook instance;
    private static Context applicationContext = null;

    private KGPTBrain brain;
    private HookManager hookManager;
    private Class<?> inputConnectionClass = null;
    private Class<?> inputMethodServiceClass = null;
    // Exact members hooked for the current InputConnection implementation —
    // unhook by identity, because the fuzzy finder may resolve methods declared
    // on superclasses (declaringClass would never match the runtime class).
    private final java.util.Set<Member> lastHookedICMembers = new java.util.HashSet<>();

    // Performance optimization: Cache to avoid redundant re-hooking
    private Class<?> lastHookedInputConnectionClass = null;
    private long lastHookTime = 0;
    private static final long MIN_HOOK_INTERVAL_MS = 500;

    public MainHook() {
        super();
        instance = this;
    }

    public static MainHook getInstance() {
        return instance;
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        instance = this;

        if (!param.isFirstPackage()) {
            return;
        }

        String packageName = param.getPackageName();
        ClassLoader classLoader = param.getDefaultClassLoader();

        // NOTE: Under the Modern Xposed API (minApiVersion=100) module apps are
        // no longer hooked by themselves, so this callback never fires for our
        // own package. Module activation status is detected via XposedService
        // binding instead (see HomeFragment.checkModuleStatus).

        if ("android".equals(packageName)) {
            MainHook.log("Hooking Android System Framework (system_server)");
            if (hookManager == null) {
                hookManager = new HookManager();
            }
            try {
                tn.eluea.kgpt.hook.SystemFrameworkHook.hook(hookManager, classLoader);
            } catch (Throwable t) {
                MainHook.log("Failed to initialize SystemFrameworkHook: " + t.getMessage());
            }
            return;
        }

        if ("com.google.android.youtube".equals(packageName) || "com.google.android.apps.youtube.music".equals(packageName)) {
            MainHook.log("Hooking YouTube / YouTube Music for KGPT Downloader Integration");
            if (hookManager == null) {
                hookManager = new HookManager();
            }
            YouTubeHook.hook(hookManager, classLoader, packageName);
            return;
        }

        MainHook.log("Loading KGPT for package " + packageName);

        if (hookManager == null) {
            hookManager = new HookManager();
        }

        // Hook text selection for AI actions
        TextSelectionHook.hook(hookManager, classLoader);

        hookKeyboard(classLoader);
    }

    private void ensureInitialized(Context applicationContext) {
        if (MainHook.applicationContext == null && applicationContext != null) {
            MainHook.applicationContext = applicationContext;

            // Report FIRST: nothing above it can throw and suppress the proof
            // that LSPosed really loaded us into this process.
            reportActivationHeartbeat(applicationContext);

            SPManager.init(applicationContext);
            UiInteractor.init(applicationContext);
        }
        // Recreate the brain on every IMS (re)creation: onDestroy nulls it, and
        // gating on applicationContext (set once) left it dead forever after
        // the first keyboard restart.
        if (brain == null && MainHook.applicationContext != null) {
            brain = new KGPTBrain(MainHook.applicationContext);
        }
    }

    // Activation heartbeat: this code only ever runs inside a HOOKED process
    // (keyboard app) when LSPosed actually loaded the module, so writing it is
    // direct proof of real activation. The module app's status card reads it
    // via the exported ConfigProvider as a second, ground-truth signal beside
    // XposedService binding.
    private static boolean heartbeatReported = false;

    private static void reportActivationHeartbeat(Context context) {
        if (heartbeatReported || context == null) return;
        heartbeatReported = true;
        try {
            // P1: Remote Preferences (API 102) — framework-managed, no
            // world-readable files, delivered to the app via XposedService.
            try {
                android.content.SharedPreferences remote =
                        getInstance().getRemotePreferences("kgpt_heartbeat");
                remote.edit()
                        .putLong("ts", System.currentTimeMillis())
                        .putString("pkg", context.getPackageName())
                        .commit();
            } catch (Throwable t2) {
                MainHook.log("remote heartbeat failed: " + t2.getMessage());
            }

            // Legacy provider path kept as fallback for older frameworks
            tn.eluea.kgpt.provider.ConfigClient client =
                    new tn.eluea.kgpt.provider.ConfigClient(context);
            try {
                client.putString("module_activation_heartbeat",
                        String.valueOf(System.currentTimeMillis()));
                client.putString("module_activation_process",
                        context.getPackageName());
            } finally {
                client.destroy();
            }
            MainHook.log("Activation heartbeat reported from " + context.getPackageName());
        } catch (Throwable t) {
            MainHook.log(t);
        }
    }

    private void hookKeyboard(ClassLoader classLoader) {
        hookManager.hook(
                InputMethodService.class,
                "onCreate",
                new Class<?>[]{},
                MethodHook.after(param -> {
                    MainHook.log("InputMethodService onCreate");
                    InputMethodService ims = (InputMethodService) param.getThisObject();
                    ensureInitialized(ims.getApplicationContext());
                    UiInteractor.getInstance().onInputMethodCreate(ims);

                    inputMethodServiceClass = ims.getClass();
                    MainHook.log("InputMethodService : " + inputMethodServiceClass.getName());

                    hookMethodService();
                })
        );

        hookManager.hook(
                InputMethodService.class,
                "onDestroy",
                new Class<?>[]{},
                MethodHook.before(param -> {
                    MainHook.log("InputMethodService onDestroy");
                    InputMethodService ims = (InputMethodService) param.getThisObject();
                    UiInteractor.getInstance().onInputMethodDestroy(ims);

                    if (brain != null) {
                        brain.destroy();
                        brain = null;
                    }

                    lastHookedInputConnectionClass = null;
                    lastHookTime = 0;
                })
        );

        hookManager.hook(
                InputMethodService.class,
                "onFinishInput",
                new Class<?>[]{},
                MethodHook.before(param -> MainHook.log("InputMethodService onFinishInput"))
        );

        hookManager.hook(
                Instrumentation.class,
                "callApplicationOnCreate",
                new Class<?>[]{Application.class},
                MethodHook.before(param -> {
                    Application app = (Application) param.getArgs()[0];
                    if (app != null) {
                        ensureInitialized(app.getApplicationContext());
                    }
                })
        );
    }

    private void hookMethodService() {
        // P2: deoptimize the proven-critical selection path — prevents ART
        // inlining from ever silencing this hook on aggressive OEM builds.
        // (InputConnection commit hooks are deliberately NOT deoptimized:
        // they fire on every commit and interpreter mode would add typing
        // latency; virtual interface dispatch keeps them hookable.)
        try {
            deoptimizeIfPossible(HookManager.findMethod(inputMethodServiceClass,
                    "onUpdateSelection",
                    new Class<?>[]{int.class, int.class, int.class, int.class, int.class, int.class}));
            deoptimizeIfPossible(HookManager.findMethod(inputMethodServiceClass,
                    "onStartInput", new Class<?>[]{EditorInfo.class, boolean.class}));
        } catch (Throwable ignored) {}

        hookManager.hook(
                inputMethodServiceClass,
                "onUpdateSelection",
                new Class<?>[]{int.class, int.class, int.class, int.class, int.class, int.class},
                MethodHook.after(param -> {
                    InputMethodService ims = (InputMethodService) param.getThisObject();
                    if (ims.getCurrentInputEditorInfo() != null) {
                        String packageName = ims.getCurrentInputEditorInfo().packageName;
                        if (BuildConfig.APPLICATION_ID.equals(packageName)) {
                            return;
                        }
                    }

                    int oldSelStart = (int) param.getArgs()[0];
                    int oldSelEnd = (int) param.getArgs()[1];
                    int newSelStart = (int) param.getArgs()[2];
                    int newSelEnd = (int) param.getArgs()[3];

                    IMSController.getInstance().onUpdateSelection(
                            oldSelStart,
                            oldSelEnd,
                            newSelStart,
                            newSelEnd,
                            (int) param.getArgs()[4],
                            (int) param.getArgs()[5]
                    );

                    if (brain != null && brain.getSelectionHandler() != null) {
                        brain.getSelectionHandler().onSelectionChanged(
                                ims, oldSelStart, oldSelEnd, newSelStart, newSelEnd
                        );
                    }
                })
        );

        hookManager.hook(
                inputMethodServiceClass,
                "onStartInput",
                new Class<?>[]{EditorInfo.class, boolean.class},
                MethodHook.after(param -> {
                    InputMethodService ims = (InputMethodService) param.getThisObject();
                    if (ims.getCurrentInputConnection() == null) {
                        return;
                    }

                    Class<?> newInputConnectionClass = ims.getCurrentInputConnection().getClass();
                    long currentTime = System.currentTimeMillis();

                    if (newInputConnectionClass.equals(lastHookedInputConnectionClass)
                            && (currentTime - lastHookTime) < MIN_HOOK_INTERVAL_MS) {
                        return;
                    }

                    if (!newInputConnectionClass.equals(lastHookedInputConnectionClass)) {
                        final java.util.Set<Member> stale = new java.util.HashSet<>(lastHookedICMembers);
                        hookManager.unhook(stale::contains);
                        MainHook.log("InputMethodService onStartInput");
                        inputMethodServiceClass = ims.getClass();
                        inputConnectionClass = newInputConnectionClass;
                        lastHookedInputConnectionClass = newInputConnectionClass;
                        MainHook.log("InputMethodService InputConnection : " + inputConnectionClass.getName());

                        hookInputConnection();
                    }

                    lastHookTime = currentTime;
                })
        );
        MainHook.log("Done hooking InputMethodService : " + inputMethodServiceClass.getName());
    }

    @SuppressLint("ObsoleteSdkInt")
    private void hookInputConnection() {
        lastHookedICMembers.clear();
        MethodHook conditionalGate = MethodHook.before(param -> {
            if (IMSController.getInstance().isInputLocked()) {
                param.setResult(false);
            }
        });

        hookManager.hook(inputConnectionClass, "commitText",
                new Class<?>[]{CharSequence.class, int.class}, conditionalGate);
        hookManager.hook(inputConnectionClass, "commitCorrection",
                new Class<?>[]{android.view.inputmethod.CorrectionInfo.class}, conditionalGate);
        hookManager.hook(inputConnectionClass, "commitCompletion",
                new Class<?>[]{android.view.inputmethod.CompletionInfo.class}, conditionalGate);
        hookManager.hook(inputConnectionClass, "setComposingText",
                new Class<?>[]{CharSequence.class, int.class}, conditionalGate);
        hookManager.hook(inputConnectionClass, "finishComposingText",
                new Class<?>[]{}, conditionalGate);
        hookManager.hook(inputConnectionClass, "deleteSurroundingText",
                new Class<?>[]{int.class, int.class}, conditionalGate);

        if (Build.VERSION.SDK_INT >= 24) {
            hookManager.hook(inputConnectionClass, "deleteSurroundingTextInCodePoints",
                    new Class<?>[]{int.class, int.class}, conditionalGate);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            hookManager.hook(inputConnectionClass, "commitText",
                    new Class<?>[]{CharSequence.class, int.class,
                            android.view.inputmethod.TextAttribute.class},
                    conditionalGate);
        }
        if (Build.VERSION.SDK_INT >= 34) {
            hookManager.hook(inputConnectionClass, "replaceText",
                    new Class<?>[]{int.class, int.class, CharSequence.class, int.class,
                            android.view.inputmethod.TextAttribute.class},
                    conditionalGate);
        }

        // Snapshot exactly which Members got hooked for precise unhook later
        for (Member m : hookManager.getHookedMembers()) {
            if (m.getDeclaringClass().equals(inputConnectionClass)
                    || inputConnectionClass.isAssignableFrom(m.getDeclaringClass())
                    || m.getDeclaringClass().isAssignableFrom(inputConnectionClass)) {
                lastHookedICMembers.add(m);
            }
        }
        MainHook.log("Done hooking InputConnection : " + inputConnectionClass.getName()
                + " (" + lastHookedICMembers.size() + " methods)");
    }

    /**
     * P2 (API 102): force ART to interpret a method so hot system hooks can
     * never be inlined away by OEM optimisations. Safe no-op on failure.
     */
    public static void deoptimizeIfPossible(Member member) {
        try {
            if (member instanceof java.lang.reflect.Executable && getInstance() != null) {
                getInstance().deoptimize((java.lang.reflect.Executable) member);
            }
        } catch (Throwable ignored) {}
    }

    public static void logST() {
        log(Log.getStackTraceString(new Throwable()));
    }

    public static void log(String message) {
        tn.eluea.kgpt.util.Logger.log(message);
    }

    public static void log(Throwable t) {
        tn.eluea.kgpt.util.Logger.log(t);
        UiInteractor.getInstance().post(
                () -> UiInteractor.getInstance().toastLong(t.getClass().getSimpleName() + " : " + t.getMessage())
        );
    }
}
