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
package tn.eluea.kgpt.hook;

import android.os.Build;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tn.eluea.kgpt.MainHook;

public class HookManager {
    private Map<Method, XC_MethodHook.Unhook> unhookMap = new HashMap<>();

    public void hook(Class<?> clazz, String methodName, Class<?>[] paramTypes, XC_MethodHook callback) {
        Method method = findMethod(clazz, methodName, paramTypes);
        if (!unhookMap.containsKey(method)) {
            unhookMap.put(method, XposedBridge.hookMethod(method, callback));
        }
    }

    /*
    * Deprecated because using getDeclaredMethods in Gboard can cause NoClassDefFoundError
    * */
    @Deprecated
    public void hookAll(Class<?> clazz, String methodName, XC_MethodHook callback) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                if (!unhookMap.containsKey(method)) {
                    unhookMap.put(method, XposedBridge.hookMethod(method, callback));
                }
            }
        }
    }

    public void unhook(Predicate<Method> clearPredicate) {
        for (Method method: unhookMap.keySet()) {
            if (clearPredicate.test(method)) {
                unhookMap.remove(method).unhook();
            }
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            return XposedHelpers.findMethodBestMatch(clazz, methodName, paramTypes);
        } catch (Throwable e) {
            MainHook.log("XposedHelpers API could not find " + clazz.getName() + "."  + methodName
                    + " because of (" +
                    e.getClass().getName() + " : " + e.getMessage() +
                    "). Falling back to standard java API");

            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException(e2);
            }
        }
    }
}
