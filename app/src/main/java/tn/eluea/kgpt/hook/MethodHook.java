/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.hook;

import java.lang.reflect.Member;
import java.util.function.Consumer;

public class MethodHook {
    public static class MethodHookParam {
        private final Member method;
        private final Object thisObject;
        private Object[] args;
        private Object result = null;
        private Throwable throwable = null;
        private boolean returnEarly = false;

        public MethodHookParam(Member method, Object thisObject, Object[] args) {
            this.method = method;
            this.thisObject = thisObject;
            this.args = args != null ? args : new Object[0];
        }

        public Member getMethod() {
            return method;
        }

        public Object getThisObject() {
            return thisObject;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public boolean isReturnEarly() {
            return returnEarly;
        }
    }

    private final Consumer<MethodHookParam> before;
    private final Consumer<MethodHookParam> after;

    public MethodHook(Consumer<MethodHookParam> before, Consumer<MethodHookParam> after) {
        this.before = before;
        this.after = after;
    }

    public void callBefore(MethodHookParam param) {
        if (before != null) {
            try {
                before.accept(param);
            } catch (Throwable t) {
                tn.eluea.kgpt.util.Logger.log("Hook callback failed: " + t);
            }
        }
    }

    public void callAfter(MethodHookParam param) {
        if (after != null) {
            try {
                after.accept(param);
            } catch (Throwable t) {
                tn.eluea.kgpt.util.Logger.log("Hook callback failed: " + t);
            }
        }
    }

    public static MethodHook after(Consumer<MethodHookParam> after) {
        return new MethodHook(null, after);
    }

    public static MethodHook before(Consumer<MethodHookParam> before) {
        return new MethodHook(before, null);
    }
}
