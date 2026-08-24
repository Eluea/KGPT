/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.hook;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;


import io.github.libxposed.api.XposedInterface;
import tn.eluea.kgpt.MainHook;

public class HookManager {
    private static final Map<Member, MethodHook> hooksMap = new ConcurrentHashMap<>();
    // Live libxposed handles: re-hooking replaces (unhook old first) instead of
    // stacking interceptors, and unhook() actually detaches the native hook.
    private static final Map<Member, Object> hookHandles = new ConcurrentHashMap<>();

    public void hook(Class<?> clazz, String methodName, Class<?>[] paramTypes, MethodHook callback) {
        Method method = findMethod(clazz, methodName, paramTypes);
        if (method != null) {
            hook(method, callback);
        }
    }

    @SuppressWarnings("unchecked")
    public void hook(Method method, MethodHook callback) {
        if (method == null) return;

        hooksMap.put(method, callback);
        MainHook mainHook = MainHook.getInstance();
        if (mainHook != null) {
            try {
                // Replace any previously installed intercept for this method:
                // stacking interceptors made every callback fire N times.
                Object existing = hookHandles.get(method);
                if (existing instanceof io.github.libxposed.api.XposedInterface.HookHandle) {
                    ((io.github.libxposed.api.XposedInterface.HookHandle) existing).unhook();
                    hookHandles.remove(method);
                }

                Object handle = mainHook.hook(method).intercept(chain -> {
                    List<Object> originalArgs = chain.getArgs();
                    Object[] argsArray = originalArgs != null ? originalArgs.toArray() : new Object[0];

                    MethodHook hook = hooksMap.get(method);
                    if (hook == null) {
                        return chain.proceed(argsArray);
                    }

                    MethodHook.MethodHookParam param = new MethodHook.MethodHookParam(
                            method,
                            chain.getThisObject(),
                            argsArray
                    );

                    hook.callBefore(param);

                    if (param.isReturnEarly()) {
                        if (param.hasThrowable()) {
                            throw param.getThrowable();
                        }
                        return param.getResult();
                    }

                    Object result = null;
                    try {
                        Object[] newArgs = param.getArgs() != null ? param.getArgs() : argsArray;
                        result = chain.proceed(newArgs);
                        param.setResult(result);
                    } catch (Throwable t) {
                        param.setThrowable(t);
                    }

                    hook.callAfter(param);

                    if (param.hasThrowable()) {
                        throw param.getThrowable();
                    }
                    return param.getResult();
                });
                hookHandles.put(method, handle);
            } catch (Throwable t) {
                MainHook.log("Failed to hook method " + method.getName() + ": " + t.getMessage());
            }
        }
    }

    public java.util.Set<Member> getHookedMembers() {
        return hooksMap.keySet();
    }

    public void unhook(Predicate<Member> clearPredicate) {
        for (Member m : hooksMap.keySet().toArray(new Member[0])) {
            if (!clearPredicate.test(m)) continue;
            hooksMap.remove(m);
            Object handle = hookHandles.remove(m);
            if (handle instanceof io.github.libxposed.api.XposedInterface.HookHandle) {
                try {
                    ((io.github.libxposed.api.XposedInterface.HookHandle) handle).unhook();
                } catch (Throwable t) {
                    MainHook.log("unhook failed for " + m.getName() + ": " + t.getMessage());
                }
            }
        }
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        if (clazz == null || methodName == null) return null;
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes != null ? paramTypes : new Class<?>[0]);
        } catch (NoSuchMethodException e) {
            try {
                return clazz.getMethod(methodName, paramTypes != null ? paramTypes : new Class<?>[0]);
            } catch (NoSuchMethodException e2) {
                // Fuzzy match fallback
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        if (paramTypes == null || m.getParameterCount() == paramTypes.length) {
                            return m;
                        }
                    }
                }
                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(methodName)) {
                        if (paramTypes == null || m.getParameterCount() == paramTypes.length) {
                            return m;
                        }
                    }
                }
            }
        }
        return null;
    }
}
