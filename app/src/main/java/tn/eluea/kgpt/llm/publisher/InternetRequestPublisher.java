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
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import android.util.Log;

import tn.eluea.kgpt.llm.service.InternetRequestListener;

public class InternetRequestPublisher implements
        Publisher<String>, InternetRequestListener {
    private static final String TAG = "KGPT_InternetPub";
    private final AtomicInteger mStatusCode = new AtomicInteger(-1);
    private final Object mLock = new Object();
    private final Callback mOnStatusCodeSuccess;
    private final Callback mOnStatusCodeError;
    private InputStream mInputStream = null;

    public InternetRequestPublisher(Callback onStatusCodeSuccess,
                                    Callback onStatusCodeError) {
        mOnStatusCodeSuccess = onStatusCodeSuccess;
        mOnStatusCodeError = onStatusCodeError;
    }

    @Override
    public void subscribe(Subscriber<? super String> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                    return;
                }

                synchronized (mLock) {
                    while (mStatusCode.get() == -1) {
                        Log.d(TAG, "Waiting for status code");
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                Log.d(TAG, "Received status code " + mStatusCode);
                boolean hasError = false;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
                    if (mStatusCode.get() == 200) {
                        mOnStatusCodeSuccess.callback(subscriber, reader);
                    } else {
                        mOnStatusCodeError.callback(subscriber, reader);
                    }
                    reader.close();
                    mInputStream.close();
                } catch (Throwable t) {
                    Log.e(TAG, "Error", t);
                    hasError = true;
                    subscriber.onError(t);
                }

                if (!hasError) {
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    @Override
    public void onRequestStatusCode(int code) {
        synchronized (mLock) {
            mStatusCode.set(code);
            mLock.notifyAll();
        }
    }

    @Override
    public void onRequestComplete() {

    }

    public void setInputStream(InputStream inputStream) {
        mInputStream = inputStream;
    }

    public interface Callback {
        void callback(Subscriber<? super String> subscriber, BufferedReader reader) throws Throwable;
    }
}
