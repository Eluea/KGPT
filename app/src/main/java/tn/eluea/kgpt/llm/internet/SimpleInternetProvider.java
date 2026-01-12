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
package tn.eluea.kgpt.llm.internet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

import tn.eluea.kgpt.llm.service.InternetRequestListener;

public class SimpleInternetProvider implements InternetProvider {
    private static final String TAG = "KGPT_SimpleInternet";
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public InputStream sendRequest(HttpURLConnection con, String body, InternetRequestListener irl) throws IOException {
        Log.d(TAG, "Sending request to " + con.getURL());

        con.setDoOutput(true);
        con.setConnectTimeout(30000);
        con.setReadTimeout(60000);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        Log.d(TAG, "Response code = " + responseCode);
        irl.onRequestStatusCode(responseCode);

        // Handle error responses immediately
        if (responseCode >= 400) {
            String errorMessage = readStreamFully(con.getErrorStream());
            Log.e(TAG, "Request failed with code " + responseCode + ": " + errorMessage);
            throw new IOException("API Error " + responseCode + ": " + errorMessage);
        }

        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(inputStream);

        // For successful responses, stream the content
        InputStream responseStream = con.getInputStream();

        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.write((line + System.lineSeparator()).getBytes());
                    outputStream.flush();
                }
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error streaming response: " + e.getMessage());
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        });

        return inputStream;
    }

    private String readStreamFully(InputStream stream) {
        if (stream == null)
            return "Unknown Error";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            return result.toString();
        } catch (IOException e) {
            return "Error reading error stream: " + e.getMessage();
        }
    }
}
