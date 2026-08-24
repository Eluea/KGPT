/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import android.util.Log;

import tn.eluea.kgpt.llm.service.InternetRequestListener;

public class InternetRequestPublisher implements
        Publisher<String>, InternetRequestListener {
    private static final String TAG = "KGPT_InternetPub";
    private static final long RESPONSE_TIMEOUT_MS = 60_000;

    private final AtomicInteger mStatusCode = new AtomicInteger(-1);
    private final AtomicReference<Throwable> mFatalError = new AtomicReference<>(null);
    private volatile boolean mCancelled = false;
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

                // Bounded wait: never block forever if the remote side dies
                // without replying (previously this waited indefinitely and
                // starved the caller's worker threads).
                long deadline = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;
                synchronized (mLock) {
                    while (mStatusCode.get() == -1 && mFatalError.get() == null && !mCancelled) {
                        long remaining = deadline - System.currentTimeMillis();
                        if (remaining <= 0) {
                            Log.e(TAG, "Timed out waiting for HTTP status");
                            subscriber.onError(new IOException("KGPT internet proxy: timed out waiting for response"));
                            return;
                        }
                        try {
                            mLock.wait(Math.min(remaining, 5_000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    Throwable fatal = mFatalError.get();
                    if (fatal != null) {
                        subscriber.onError(fatal);
                        return;
                    }
                    if (mCancelled) {
                        return;
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
                mCancelled = true;
                synchronized (mLock) {
                    mLock.notifyAll();
                }
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

    @Override
    public void onRequestError(Throwable t) {
        mFatalError.set(t);
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    public void setInputStream(InputStream inputStream) {
        mInputStream = inputStream;
    }

    public interface Callback {
        void callback(Subscriber<? super String> subscriber, BufferedReader reader) throws Throwable;
    }
}
