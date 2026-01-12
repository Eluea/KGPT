/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * This file is part of KGPT.
 * Based on original code from KeyboardGPT by Mino260806.
 * Original: https://github.com/Mino260806/KeyboardGPT
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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tn.eluea.kgpt.hook.HookManager;
import tn.eluea.kgpt.hook.MethodHook;
import tn.eluea.kgpt.hook.TextSelectionHook;
import tn.eluea.kgpt.provider.XposedConfigReader;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;

public class MainHook implements IXposedHookLoadPackage {
    private static Context applicationContext = null;

    private KGPTBrain brain;

    private HookManager hookManager;

    private Class<?> inputConnectionClass = null;

    private Class<?> inputMethodServiceClass = null;
    
    // Performance optimization: Cache to avoid redundant re-hooking
    private Class<?> lastHookedInputConnectionClass = null;
    private long lastHookTime = 0;
    private static final long MIN_HOOK_INTERVAL_MS = 500; // Minimum interval between hooks

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.isFirstApplication) {
            return;
        }

        if (lpparam.packageName.equals("tn.eluea.kgpt")) {
            MainHook.log("Hooking own module for status check");
            // Hook the module status check method
            XposedHelpers.findAndHookMethod(
                    "tn.eluea.kgpt.ui.main.fragments.HomeFragment",
                    lpparam.classLoader,
                    "isModuleActiveInternal",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    });
            return;
        }

        MainHook.log("Loading KGPT for package " + lpparam.packageName);

        // Log XSharedPreferences status early
        MainHook.log("XSharedPreferences available: " + XposedConfigReader.isAvailable());
        MainHook.log(XposedConfigReader.getDebugInfo());

        // Hook text selection for AI actions (works in any app)
        TextSelectionHook.hook(lpparam);

        hookKeyboard(lpparam);
    }

    private void ensureInitialized(Context applicationContext) {
        if (MainHook.applicationContext == null) {
            MainHook.applicationContext = applicationContext;

            SPManager.init(applicationContext);
            UiInteractor.init(applicationContext);

            brain = new KGPTBrain(applicationContext);
        }
    }

    private void hookKeyboard(XC_LoadPackage.LoadPackageParam lpparam) {
        hookManager = new HookManager();

        XposedHelpers.findAndHookMethod(InputMethodService.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                MainHook.log("InputMethodService onCreate");
                InputMethodService ims = (InputMethodService) param.thisObject;

                ensureInitialized(ims.getApplicationContext());

                UiInteractor.getInstance().onInputMethodCreate(ims);

                inputMethodServiceClass = ims.getClass();
                MainHook.log("InputMethodService : " + inputMethodServiceClass.getName());

                hookMethodService();
            }
        });

        XposedHelpers.findAndHookMethod(InputMethodService.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MainHook.log("InputMethodService onDestroy");
                InputMethodService ims = (InputMethodService) param.thisObject;
                UiInteractor.getInstance().onInputMethodDestroy(ims);
                
                // Clean up brain resources
                if (brain != null) {
                    brain.destroy();
                    brain = null;
                }
                
                // Reset hook cache
                lastHookedInputConnectionClass = null;
                lastHookTime = 0;
            }
        });

        XposedHelpers.findAndHookMethod(InputMethodService.class, "onFinishInput", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MainHook.log("InputMethodService onFinishInput");
            }
        });

        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate",
                Application.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Application app = (Application) param.args[0];
                        ensureInitialized(app.getApplicationContext());
                    }
                });
    }

    private void hookMethodService() {
        hookManager.hook(inputMethodServiceClass, "onUpdateSelection",
                new Class<?>[] { int.class, int.class, int.class, int.class, int.class, int.class },
                MethodHook.after(param -> {
                    InputMethodService ims = (InputMethodService) param.thisObject;
                    String packageName = ims.getCurrentInputEditorInfo().packageName;
                    if (BuildConfig.APPLICATION_ID.equals(packageName)) {
                        return;
                    }

                    int oldSelStart = (int) param.args[0];
                    int oldSelEnd = (int) param.args[1];
                    int newSelStart = (int) param.args[2];
                    int newSelEnd = (int) param.args[3];

                    // Notify IMSController for text parsing
                    IMSController.getInstance().onUpdateSelection(
                            oldSelStart,
                            oldSelEnd,
                            newSelStart,
                            newSelEnd,
                            (int) param.args[4],
                            (int) param.args[5]);

                    // Notify SelectionHandler for text actions
                    if (brain != null && brain.getSelectionHandler() != null) {
                        brain.getSelectionHandler().onSelectionChanged(
                                ims, oldSelStart, oldSelEnd, newSelStart, newSelEnd);
                    }
                }));

        hookManager.hook(inputMethodServiceClass, "onStartInput",
                new Class<?>[] { EditorInfo.class, boolean.class }, MethodHook.after(param -> {
                    InputMethodService ims = (InputMethodService) param.thisObject;
                    
                    // Performance optimization: Skip if InputConnection hasn't changed
                    if (ims.getCurrentInputConnection() == null) {
                        return;
                    }
                    
                    Class<?> newInputConnectionClass = ims.getCurrentInputConnection().getClass();
                    long currentTime = System.currentTimeMillis();
                    
                    // Skip re-hooking if same class and within minimum interval
                    if (newInputConnectionClass.equals(lastHookedInputConnectionClass) 
                            && (currentTime - lastHookTime) < MIN_HOOK_INTERVAL_MS) {
                        return;
                    }
                    
                    // Only unhook and rehook if the class actually changed
                    if (!newInputConnectionClass.equals(lastHookedInputConnectionClass)) {
                        hookManager.unhook(m -> m.getClass().equals(inputConnectionClass));
                        
                        MainHook.log("InputMethodService onStartInput");
                        inputMethodServiceClass = ims.getClass();
                        inputConnectionClass = newInputConnectionClass;
                        lastHookedInputConnectionClass = newInputConnectionClass;
                        MainHook.log("InputMethodService InputConnection : " + inputConnectionClass.getName());
                        
                        hookInputConnection();
                    }
                    
                    lastHookTime = currentTime;
                }));
        MainHook.log("Done hooking InputMethodService : " + inputMethodServiceClass.getName());
    }

    @SuppressLint("ObsoleteSdkInt")
    private void hookInputConnection() {
        XC_MethodHook conditionalGate = MethodHook.before(param -> {
            if (IMSController.getInstance().isInputLocked()) {
                param.setResult(false);
            }
        });

        hookManager.hook(inputConnectionClass, "commitText",
                new Class<?>[] { CharSequence.class, int.class }, conditionalGate);
        hookManager.hook(inputConnectionClass, "commitCorrection",
                new Class<?>[] { android.view.inputmethod.CorrectionInfo.class }, conditionalGate);
        hookManager.hook(inputConnectionClass, "commitCompletion",
                new Class<?>[] { android.view.inputmethod.CompletionInfo.class }, conditionalGate);
        hookManager.hook(inputConnectionClass, "setComposingText",
                new Class<?>[] { CharSequence.class, int.class }, conditionalGate);
        hookManager.hook(inputConnectionClass, "finishComposingText",
                new Class<?>[] {}, conditionalGate);
        hookManager.hook(inputConnectionClass, "deleteSurroundingText",
                new Class<?>[] { int.class, int.class }, conditionalGate);

        if (Build.VERSION.SDK_INT >= 24) {
            hookManager.hook(inputConnectionClass, "deleteSurroundingTextInCodePoints",
                    new Class<?>[] { int.class, int.class }, conditionalGate);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            hookManager.hook(inputConnectionClass, "commitText",
                    new Class<?>[] { CharSequence.class, int.class,
                            android.view.inputmethod.TextAttribute.class },
                    conditionalGate);
        }
        if (Build.VERSION.SDK_INT >= 34) {
            hookManager.hook(inputConnectionClass, "replaceText",
                    new Class<?>[] { int.class, int.class, CharSequence.class, int.class,
                            android.view.inputmethod.TextAttribute.class },
                    conditionalGate);
        }

        MainHook.log("Done hooking InputConnection : " + inputConnectionClass.getName());
    }

    // Flag to check if we're in Xposed context
    private static final boolean IS_XPOSED_CONTEXT;
    static {
        boolean xposedAvailable = false;
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            xposedAvailable = true;
        } catch (ClassNotFoundException e) {
            xposedAvailable = false;
        }
        IS_XPOSED_CONTEXT = xposedAvailable;
    }

    public static void logST() {
        log(Log.getStackTraceString(new Throwable()));
    }

    public static void log(String message) {
        tn.eluea.kgpt.util.Logger.log(message);
    }

    public static void log(Throwable t) {
        // In app context, use Android Log
        if (!IS_XPOSED_CONTEXT) {
            Log.e("KGPT", "Error", t);
            return;
        }

        // In Xposed context, use XposedBridge.log
        XposedBridge.log(t);

        UiInteractor.getInstance().post(
                () -> UiInteractor.getInstance().toastLong(t.getClass().getSimpleName() + " : " + t.getMessage()));
    }
}
