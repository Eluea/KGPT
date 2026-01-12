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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Security verification for downloaded APK files.
 * 
 * Verifies:
 * 1. SHA256 Checksum - Ensures file integrity
 * 2. APK Signature - Ensures APK is signed by the same developer
 */
public class UpdateSecurityVerifier {

    private static final String TAG = "KGPT_SecurityVerifier";

    private final Context context;

    public UpdateSecurityVerifier(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Verify downloaded APK file.
     * 
     * @param apkFile          The downloaded APK file
     * @param expectedChecksum Expected SHA256 checksum (from release notes)
     * @return VerificationResult indicating success or failure reason
     */
    public VerificationResult verifyApk(File apkFile, String expectedChecksum) {
        Log.d(TAG, "Starting APK verification: " + apkFile.getName());

        // Step 1: Verify file exists and is readable
        if (!apkFile.exists() || !apkFile.canRead()) {
            return new VerificationResult(false, "APK file not found or not readable");
        }

        // Step 2: Verify checksum (if provided)
        if (expectedChecksum != null && !expectedChecksum.isEmpty()) {
            String actualChecksum = calculateSha256(apkFile);
            if (actualChecksum == null) {
                return new VerificationResult(false, "Failed to calculate checksum");
            }

            if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
                Log.e(TAG, "Checksum mismatch! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
                return new VerificationResult(false, "Checksum verification failed - File may be corrupted");
            }

            Log.i(TAG, "Checksum verified successfully");
        } else {
            Log.w(TAG, "No checksum provided, skipping checksum verification");
        }

        // Step 3: Verify APK signature matches current app
        if (!verifySignature(apkFile)) {
            return new VerificationResult(false,
                    "Signature verification failed - APK may not be from the original developer");
        }

        Log.i(TAG, "APK verification completed successfully");
        return new VerificationResult(true, "Verification successful");
    }

    /**
     * Calculate SHA256 checksum of a file
     */
    public String calculateSha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error calculating SHA256", e);
            return null;
        }
    }

    /**
     * Verify that the downloaded APK is signed with the same key as the installed
     * app
     */
    @SuppressWarnings("deprecation")
    private boolean verifySignature(File apkFile) {
        try {
            PackageManager pm = context.getPackageManager();

            // Get current app's signature
            PackageInfo currentInfo = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES);

            if (currentInfo.signatures == null || currentInfo.signatures.length == 0) {
                Log.e(TAG, "Could not get current app signature");
                return false;
            }

            Signature[] currentSignatures = currentInfo.signatures;

            // Get downloaded APK's signature
            PackageInfo apkInfo = pm.getPackageArchiveInfo(
                    apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNATURES);

            if (apkInfo == null) {
                Log.e(TAG, "Could not parse downloaded APK");
                return false;
            }

            if (apkInfo.signatures == null || apkInfo.signatures.length == 0) {
                Log.e(TAG, "Downloaded APK has no signature");
                return false;
            }

            Signature[] apkSignatures = apkInfo.signatures;

            // Compare signatures
            if (currentSignatures.length != apkSignatures.length) {
                Log.e(TAG, "Signature count mismatch");
                return false;
            }

            for (int i = 0; i < currentSignatures.length; i++) {
                if (!currentSignatures[i].equals(apkSignatures[i])) {
                    Log.e(TAG, "Signature mismatch at index " + i);
                    return false;
                }
            }

            Log.i(TAG, "Signature verification passed");
            return true;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found during signature verification", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error during signature verification", e);
            return false;
        }
    }

    /**
     * Get the SHA256 fingerprint of the current app's signing certificate
     */
    @SuppressWarnings("deprecation")
    public String getCurrentAppFingerprint() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES);

            if (info.signatures != null && info.signatures.length > 0) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(info.signatures[0].toByteArray());
                return bytesToHex(digest);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app fingerprint", e);
        }
        return null;
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Result of APK verification
     */
    public static class VerificationResult {
        public final boolean success;
        public final String message;

        public VerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        @Override
        public String toString() {
            return "VerificationResult{success=" + success + ", message='" + message + "'}";
        }
    }
}
