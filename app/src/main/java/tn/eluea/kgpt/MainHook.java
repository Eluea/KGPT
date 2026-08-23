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

import java.lang.reflect.Method;

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

        if ("tn.eluea.kgpt".equals(packageName)) {
            MainHook.log("Hooking own module for status check");
            try {
                Class<?> homeFragmentClass = classLoader.loadClass("tn.eluea.kgpt.ui.main.fragments.HomeFragment");
                Method statusMethod = homeFragmentClass.getDeclaredMethod("isModuleActiveInternal");
                hook(statusMethod).intercept(chain -> true);
            } catch (Throwable t) {
                MainHook.log("Failed to hook isModuleActiveInternal: " + t.getMessage());
            }
            return;
        }

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
            SPManager.init(applicationContext);
            UiInteractor.init(applicationContext);
            brain = new KGPTBrain(applicationContext);
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
                        hookManager.unhook(m -> m.getDeclaringClass().equals(inputConnectionClass));
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

        MainHook.log("Done hooking InputConnection : " + inputConnectionClass.getName());
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
