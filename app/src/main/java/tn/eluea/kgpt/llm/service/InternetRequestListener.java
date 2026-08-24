/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.llm.service;

public interface InternetRequestListener {
    void onRequestStatusCode(int code);
    void onRequestComplete();

    /**
     * Called when a request fails irrecoverably (service died, timeout, ...).
     * Default no-op so existing implementors keep compiling.
     */
    default void onRequestError(Throwable t) {
    }
}
