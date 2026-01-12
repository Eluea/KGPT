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
package tn.eluea.kgpt.llm.publisher;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class ExceptionPublisher implements Publisher<String> {
    private final Throwable mThrowable;

    public ExceptionPublisher(Throwable throwable) {
        mThrowable = throwable;
    }

    @Override
    public void subscribe(Subscriber<? super String> s) {
        s.onError(mThrowable);
        s.onComplete();
    }
}
