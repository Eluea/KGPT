/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package tn.eluea.kgpt.updater;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.eluea.kgpt.BuildConfig;

/**
 * Checks for updates from GitHub Releases API.
 * 
 * Repository: Eluea/KGPT
 * API: https://api.github.com/repos/Eluea/KGPT/releases/latest
 */
public class UpdateChecker {

    private static final String TAG = "KGPT_UpdateChecker";

    // GitHub API Configuration
    private static final String GITHUB_OWNER = "Eluea";
    private static final String GITHUB_REPO = "KGPT";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO
            + "/releases/latest";

    // Regex to extract SHA256 checksum from release notes
    // Expected format: SHA256: abc123... or **SHA256:** `abc123...`
    private static final Pattern CHECKSUM_PATTERN = Pattern.compile(
            "(?:SHA256|sha256|Checksum|checksum)[:\\s]+[`]?([a-fA-F0-9]{64})[`]?",
            Pattern.CASE_INSENSITIVE);

    private final Context context;

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Check for updates synchronously. Call from background thread.
     * 
     * @return UpdateInfo if a newer version is available, null otherwise
     */
    public UpdateInfo checkForUpdate() {
        try {
            Log.d(TAG, "Checking for updates from: " + API_URL);

            JSONObject release = fetchLatestRelease();
            if (release == null) {
                Log.w(TAG, "Failed to fetch release info");
                return null;
            }

            UpdateInfo updateInfo = parseReleaseInfo(release);
            if (updateInfo == null) {
                Log.w(TAG, "Failed to parse release info");
                return null;
            }

            // Compare versions
            if (isNewerVersion(updateInfo.getCleanVersionName())) {
                Log.i(TAG, "New update available: " + updateInfo.getVersionName());
                return updateInfo;
            } else {
                Log.d(TAG, "No update needed. Current: " + BuildConfig.VERSION_NAME +
                        ", Latest: " + updateInfo.getVersionName());
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            return null;
        }
    }

    /**
     * Fetch latest release JSON from GitHub API
     */
    private JSONObject fetchLatestRelease() throws IOException, JSONException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "KGPT-Updater/" + BuildConfig.VERSION_NAME);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub API returned: " + responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONObject(response.toString());

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Parse GitHub release JSON into UpdateInfo
     */
    private UpdateInfo parseReleaseInfo(JSONObject release) throws JSONException {
        String tagName = release.optString("tag_name", "");
        String changelog = release.optString("body", "No changelog available.");
        String releaseDate = release.optString("published_at", "");

        // Find APK asset
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null || assets.length() == 0) {
            Log.w(TAG, "No assets found in release");
            return null;
        }

        String downloadUrl = null;
        long fileSize = 0;

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.optString("name", "");

            // Look for APK file (dynamic naming: KGPT-*.apk or *.apk)
            if (name.toLowerCase().endsWith(".apk")) {
                downloadUrl = asset.optString("browser_download_url", null);
                fileSize = asset.optLong("size", 0);
                Log.d(TAG, "Found APK: " + name + " (" + fileSize + " bytes)");
                break;
            }
        }

        if (downloadUrl == null) {
            Log.w(TAG, "No APK found in release assets");
            return null;
        }

        // Security: Ensure HTTPS
        if (!downloadUrl.toLowerCase().startsWith("https://")) {
            Log.e(TAG, "Security: Download URL is not HTTPS!");
            return null;
        }

        // Extract checksum from changelog
        String checksum = extractChecksum(changelog);
        if (checksum != null) {
            Log.d(TAG, "Found checksum in release notes: " + checksum.substring(0, 8) + "...");
        } else {
            Log.w(TAG, "No checksum found in release notes");
        }

        // Calculate version code from tag (e.g., v4.1.0 -> 4010)
        int versionCode = parseVersionCode(tagName);

        return new UpdateInfo(
                tagName,
                versionCode,
                changelog,
                downloadUrl,
                checksum,
                releaseDate,
                fileSize);
    }

    /**
     * Extract SHA256 checksum from release notes
     */
    private String extractChecksum(String releaseNotes) {
        if (releaseNotes == null || releaseNotes.isEmpty()) {
            return null;
        }

        Matcher matcher = CHECKSUM_PATTERN.matcher(releaseNotes);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return null;
    }

    /**
     * Parse version string to version code (e.g., "v4.1.0" -> 4010)
     */
    private int parseVersionCode(String versionTag) {
        try {
            String clean = versionTag.replaceAll("[^0-9.]", "");
            String[] parts = clean.split("\\.");

            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            return major * 1000 + minor * 10 + patch;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Compare version strings to determine if remote is newer
     */
    private boolean isNewerVersion(String remoteVersion) {
        try {
            String currentVersion = BuildConfig.VERSION_NAME;

            int currentCode = parseVersionCode(currentVersion);
            int remoteCode = parseVersionCode(remoteVersion);

            Log.d(TAG, "Version comparison: current=" + currentCode + ", remote=" + remoteCode);

            return remoteCode > currentCode;
        } catch (Exception e) {
            Log.e(TAG, "Error comparing versions", e);
            return false;
        }
    }

    /**
     * Get current app version name
     */
    public static String getCurrentVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Get GitHub repository URL
     */
    public static String getRepositoryUrl() {
        return "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO;
    }
}
