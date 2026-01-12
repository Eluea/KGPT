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

/**
 * Data class representing update information from GitHub Releases.
 */
public class UpdateInfo {

    private final String versionName; // e.g., "v4.1.0"
    private final int versionCode; // Parsed from tag or calculated
    private final String changelog; // Release body/notes
    private final String downloadUrl; // Direct APK download URL
    private final String checksum; // SHA256 extracted from release notes
    private final String releaseDate; // Publication date
    private final long fileSize; // APK size in bytes

    public UpdateInfo(String versionName, int versionCode, String changelog,
            String downloadUrl, String checksum, String releaseDate, long fileSize) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.changelog = changelog;
        this.downloadUrl = downloadUrl;
        this.checksum = checksum;
        this.releaseDate = releaseDate;
        this.fileSize = fileSize;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getCleanVersionName() {
        // Remove 'v' prefix if present
        if (versionName != null && versionName.startsWith("v")) {
            return versionName.substring(1);
        }
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFormattedFileSize() {
        if (fileSize <= 0)
            return "Unknown";
        if (fileSize < 1024)
            return fileSize + " B";
        if (fileSize < 1024 * 1024)
            return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    /**
     * Check if download URL is secure (HTTPS only)
     */
    public boolean isSecureDownload() {
        return downloadUrl != null && downloadUrl.toLowerCase().startsWith("https://");
    }

    /**
     * Check if checksum is available
     */
    public boolean hasChecksum() {
        return checksum != null && !checksum.isEmpty() && checksum.length() == 64; // SHA256 is 64 hex chars
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
                "versionName='" + versionName + '\'' +
                ", versionCode=" + versionCode +
                ", fileSize=" + getFormattedFileSize() +
                ", hasChecksum=" + hasChecksum() +
                '}';
    }
}
