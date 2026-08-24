package tn.eluea.kgpt.features.downloader.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, high-performance asynchronous thumbnail loader with LruCache.
 */
public class ThumbnailLoader {
    private static final String TAG = "KGPT_ThumbnailLoader";
    private static ThumbnailLoader instance;

    private final LruCache<String, Bitmap> memoryCache;
    private final java.util.Set<String> inFlight =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public static synchronized ThumbnailLoader getInstance() {
        if (instance == null) {
            instance = new ThumbnailLoader();
        }
        return instance;
    }

    private ThumbnailLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // Use 1/8th of available memory for cache
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void load(String url, ImageView imageView) {
        load(url, imageView, 0);
    }

    public void load(String url, ImageView imageView, int placeholderResId) {
        if (imageView == null) {
            return;
        }

        if (url == null || url.trim().isEmpty()) {
            if (placeholderResId != 0) {
                imageView.setImageResource(placeholderResId);
            }
            return;
        }

        String formattedUrl = url.trim();
        if (formattedUrl.startsWith("//")) {
            formattedUrl = "https:" + formattedUrl;
        }
        final String cleanUrl = formattedUrl;

        // 1. Check memory cache first
        Bitmap cached = memoryCache.get(cleanUrl);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageTintList(null);
            imageView.setColorFilter(null);
            imageView.setImageBitmap(cached);
            imageView.setAlpha(1f);
            return;
        }

        // 2. Set placeholder while loading
        if (placeholderResId != 0) {
            imageView.setImageResource(placeholderResId);
        }

        // Tag the view to detect if it gets recycled before download finishes
        imageView.setTag(cleanUrl);

        final WeakReference<ImageView> imageViewRef = new WeakReference<>(imageView);

        // 3. Asynchronously download and decode image
        executorService.execute(() -> {
            Bitmap downloaded = downloadBitmap(cleanUrl);
            if (downloaded != null) {
                memoryCache.put(cleanUrl, downloaded);

                mainHandler.post(() -> {
                    ImageView target = imageViewRef.get();
                    if (target != null && cleanUrl.equals(target.getTag())) {
                        target.setImageTintList(null);
                        target.setColorFilter(null);
                        target.setImageBitmap(downloaded);
                        target.setAlpha(0f);
                        target.animate().alpha(1f).setDuration(200).start();
                    }
                });
            }
        });
    }

    private Bitmap downloadBitmap(String urlString) {
        HttpURLConnection conn = null;
        InputStream in = null;
        BufferedInputStream bufferedIn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                in = conn.getInputStream();
                bufferedIn = new BufferedInputStream(in, 8192);
                return BitmapFactory.decodeStream(bufferedIn);
            } else {
                Log.w(TAG, "Thumbnail response not OK: " + responseCode + " for " + urlString);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to download thumbnail: " + urlString + " -> " + e.getMessage());
        } finally {
            if (bufferedIn != null) {
                try {
                    bufferedIn.close();
                } catch (Throwable ignored) {}
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {}
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    public void clearCache() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }
    }
}
