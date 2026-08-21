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

    public void hook(Class<?> clazz, String methodName, Class<?>[] paramTypes, MethodHook callback) {
        Method method = findMethod(clazz, methodName, paramTypes);
        if (method != null) {
            hook(method, callback);
        }
    }

    public void hook(Method method, MethodHook callback) {
        if (method == null) return;

        hooksMap.put(method, callback);
        MainHook mainHook = MainHook.getInstance();
        if (mainHook != null) {
            try {
                mainHook.hook(method).intercept(chain -> {
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
            } catch (Throwable t) {
                MainHook.log("Failed to hook method " + method.getName() + ": " + t.getMessage());
            }
        }
    }

    public void unhook(Predicate<Member> clearPredicate) {
        hooksMap.keySet().removeIf(clearPredicate);
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
