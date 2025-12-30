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

import tn.eluea.kgpt.MainHook;
import tn.eluea.kgpt.llm.service.InternetRequestListener;

public class SimpleInternetProvider implements InternetProvider {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public InputStream sendRequest(HttpURLConnection con, String body, InternetRequestListener irl) throws IOException {
        MainHook.log("SimpleInternetProvider: Sending request to " + con.getURL());
        
        con.setDoOutput(true);
        con.setConnectTimeout(30000);
        con.setReadTimeout(60000);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        MainHook.log("SimpleInternetProvider: Response code = " + responseCode);
        irl.onRequestStatusCode(responseCode);

        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(inputStream);

        // Use error stream for non-2xx responses
        InputStream responseStream = (responseCode >= 200 && responseCode < 300) 
                ? con.getInputStream() 
                : con.getErrorStream();

        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.write((line + System.lineSeparator()).getBytes());
                    outputStream.flush();
                }
                outputStream.close();
            } catch (IOException e) {
                MainHook.log("SimpleInternetProvider error: " + e.getMessage());
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
            }
        });

        return inputStream;
    }
}
