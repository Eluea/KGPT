/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.backup;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import tn.eluea.kgpt.BuildConfig;

public class LogExporter {

    private final Context context;
    private boolean hasRootAccess = false;

    public LogExporter(Context context) {
        this.context = context;
    }

    public static String generateExportFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return "KGPT_Logs_" + sdf.format(new Date()) + ".zip";
    }

    /**
     * Request root access before exporting logs
     * 
     * @return true if root access was granted
     */
    public boolean requestRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();

            int exitCode = process.waitFor();
            hasRootAccess = (exitCode == 0 && line != null && line.contains("uid=0"));
            return hasRootAccess;
        } catch (Exception e) {
            hasRootAccess = false;
            return false;
        }
    }

    public boolean hasRootAccess() {
        return hasRootAccess;
    }

    public ExportResult exportLogs(Uri uri) {
        ExportResult result = new ExportResult();

        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                ZipOutputStream zos = new ZipOutputStream(os)) {

            // Add device info
            addDeviceInfo(zos);
            result.deviceInfo = true;

            // Add Xposed logs
            addXposedLogs(zos);
            result.xposedLogs = true;

            // Add module logs (logcat for this app)
            addModuleLogs(zos);
            result.moduleLogs = true;

            // Add boot logs (dmesg/kernel) - requires root
            addBootLogs(zos);
            result.bootLogs = true;

            // Add system logs
            addSystemLogs(zos);
            result.systemLogs = true;

            // Add crash logs
            addCrashLogs(zos);
            result.crashLogs = true;

            // Add hooked keyboard info
            addHookedKeyboardInfo(zos);
            result.hookedKeyboardInfo = true;

            // Add settings dump (excluding API keys)
            addSettingsDump(zos);
            result.settingsDump = true;

            result.success = true;
            result.hasRootAccess = hasRootAccess;
            return result;
        } catch (IOException e) {
            tn.eluea.kgpt.util.Logger.log(e);
            result.success = false;
            result.errorMessage = e.getMessage();
            return result;
        }
    }

    private void addDeviceInfo(ZipOutputStream zos) throws IOException {
        StringBuilder info = new StringBuilder();
        info.append("=== KGPT Log Export ===\n");
        info.append("Export Date: ").append(new Date().toString()).append("\n");
        info.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        info.append("=== App Info ===\n");
        info.append("App Version: ").append(BuildConfig.VERSION_NAME).append("\n");
        info.append("Version Code: ").append(BuildConfig.VERSION_CODE).append("\n");
        info.append("Build Type: ").append(BuildConfig.BUILD_TYPE).append("\n\n");

        info.append("=== Device Info ===\n");
        info.append("Device: ").append(Build.DEVICE).append("\n");
        info.append("Model: ").append(Build.MODEL).append("\n");
        info.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        info.append("Brand: ").append(Build.BRAND).append("\n");
        info.append("Product: ").append(Build.PRODUCT).append("\n");
        info.append("Hardware: ").append(Build.HARDWARE).append("\n\n");

        info.append("=== Android Info ===\n");
        info.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("SDK Level: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Security Patch: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        info.append("Build ID: ").append(Build.ID).append("\n");
        info.append("Build Display: ").append(Build.DISPLAY).append("\n");
        info.append("Fingerprint: ").append(Build.FINGERPRINT).append("\n");

        addZipEntry(zos, "device_info.txt", info.toString());
    }

    private void addXposedLogs(ZipOutputStream zos) throws IOException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Xposed/LSPosed Logs ===\n");
        logs.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        // Try to get Xposed-related logs from logcat
        String xposedLogs;
        if (hasRootAccess) {
            xposedLogs = executeRootCommand(
                    "logcat -d -v time *:S XposedBridge:V EdXposed:V LSPosed:V KGPT:V KeyboardGPT:V");
        } else {
            xposedLogs = executeCommand(
                    "logcat -d -v time *:S XposedBridge:V EdXposed:V LSPosed:V KGPT:V KeyboardGPT:V");
        }

        if (xposedLogs != null && !xposedLogs.isEmpty()) {
            logs.append(xposedLogs);
        } else {
            logs.append("No Xposed logs found in logcat buffer.\n");
            logs.append("To get full Xposed logs:\n");
            logs.append("1. Open LSPosed Manager\n");
            logs.append("2. Go to Logs section\n");
            logs.append("3. Export verbose logs\n");
        }

        addZipEntry(zos, "xposed_logs.txt", logs.toString());
    }

    private void addModuleLogs(ZipOutputStream zos) throws IOException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== KGPT Module Logs ===\n");
        logs.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        // Get logs specific to this app
        String moduleLogs;
        if (hasRootAccess) {
            moduleLogs = executeRootCommand("logcat -d -v time -s KGPT:V KeyboardGPT:V KGPTHook:V MainHook:V");
        } else {
            moduleLogs = executeCommand("logcat -d -v time -s KGPT:V KeyboardGPT:V KGPTHook:V MainHook:V");
        }

        if (moduleLogs != null && !moduleLogs.isEmpty()) {
            logs.append(moduleLogs);
        } else {
            logs.append("No module-specific logs found.\n");
        }

        addZipEntry(zos, "module_logs.txt", logs.toString());
    }

    private void addBootLogs(ZipOutputStream zos) throws IOException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Boot/Kernel Logs ===\n");
        logs.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        if (hasRootAccess) {
            // With root, we can get dmesg
            String dmesgLogs = executeRootCommand("dmesg");
            if (dmesgLogs != null && !dmesgLogs.isEmpty()) {
                logs.append("=== dmesg (Kernel Messages) ===\n");
                logs.append(dmesgLogs);
            } else {
                logs.append("dmesg: No output available\n");
            }

            // Also try to get last_kmsg if available
            logs.append("\n\n=== Last Kernel Messages ===\n");
            String lastKmsg = executeRootCommand(
                    "cat /proc/last_kmsg 2>/dev/null || cat /sys/fs/pstore/console-ramoops 2>/dev/null || echo 'Not available'");
            logs.append(lastKmsg);
        } else {
            logs.append("Root access is required to read kernel logs (dmesg).\n");
            logs.append("Without root, boot logs cannot be collected.\n\n");
            logs.append("To get boot logs:\n");
            logs.append("1. Grant root access when prompted\n");
            logs.append("2. Or use ADB: adb shell dmesg\n");
        }

        // Try to get boot-related logcat entries (doesn't require root)
        logs.append("\n\n=== Boot Events (logcat) ===\n");
        String bootLogs = executeCommand("logcat -d -b events -v time");
        if (bootLogs != null && !bootLogs.isEmpty()) {
            // Limit size
            if (bootLogs.length() > 100000) {
                logs.append("[Truncated to last 100KB]\n\n");
                logs.append(bootLogs.substring(bootLogs.length() - 100000));
            } else {
                logs.append(bootLogs);
            }
        }

        addZipEntry(zos, "boot_logs.txt", logs.toString());
    }

    private void addSystemLogs(ZipOutputStream zos) throws IOException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== System Logs ===\n");
        logs.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        // Main logcat buffer
        String mainLogs;
        if (hasRootAccess) {
            mainLogs = executeRootCommand("logcat -d -v time -b main");
        } else {
            mainLogs = executeCommand("logcat -d -v time -b main");
        }

        if (mainLogs != null && !mainLogs.isEmpty()) {
            // Limit size to avoid huge files
            if (mainLogs.length() > 500000) {
                logs.append("[Truncated to last 500KB]\n\n");
                logs.append(mainLogs.substring(mainLogs.length() - 500000));
            } else {
                logs.append(mainLogs);
            }
        }

        addZipEntry(zos, "system_logs.txt", logs.toString());
    }

    private void addCrashLogs(ZipOutputStream zos) throws IOException {
        StringBuilder logs = new StringBuilder();
        logs.append("=== Crash Logs ===\n");
        logs.append("Root Access: ").append(hasRootAccess ? "Yes" : "No").append("\n\n");

        // Get crash buffer
        String crashLogs;
        if (hasRootAccess) {
            crashLogs = executeRootCommand("logcat -d -b crash -v time");
        } else {
            crashLogs = executeCommand("logcat -d -b crash -v time");
        }

        if (crashLogs != null && !crashLogs.isEmpty()) {
            logs.append(crashLogs);
        } else {
            logs.append("No crash logs found.\n");
        }

        // Also look for ANR traces
        logs.append("\n\n=== ANR Traces ===\n");
        if (hasRootAccess) {
            String anrTraces = executeRootCommand(
                    "cat /data/anr/traces.txt 2>/dev/null || echo 'No ANR traces found or permission denied'");
            logs.append(anrTraces);
        } else {
            logs.append("Root access required to read ANR traces from /data/anr/\n");
        }

        // ANR from logcat
        logs.append("\n\n=== ANR Events (logcat) ===\n");
        String anrLogs = executeCommand("logcat -d -v time -b events | grep -i anr");
        if (anrLogs != null && !anrLogs.isEmpty()) {
            logs.append(anrLogs);
        } else {
            logs.append("No ANR events found in logcat.\n");
        }

        addZipEntry(zos, "crash_logs.txt", logs.toString());
    }

    private void addHookedKeyboardInfo(ZipOutputStream zos) throws IOException {
        StringBuilder info = new StringBuilder();
        info.append("=== Hooked Keyboard Info ===\n");

        try {
            String defaultIme = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD);

            info.append("Current Default IME ID: ").append(defaultIme).append("\n");

            if (defaultIme != null && !defaultIme.isEmpty()) {
                // Determine package name. IME ID is usually "com.package.name/.ClassName"
                String packageName = defaultIme.split("/")[0];
                info.append("Target Package: ").append(packageName).append("\n");

                PackageManager pm = context.getPackageManager();
                try {
                    PackageInfo pi = pm.getPackageInfo(packageName, 0);
                    info.append("App Name: ").append(pm.getApplicationLabel(pi.applicationInfo)).append("\n");
                    info.append("Version Name: ").append(pi.versionName).append("\n");
                    info.append("Version Code: ").append(pi.versionCode).append("\n");
                    info.append("Target SDK: ").append(pi.applicationInfo.targetSdkVersion).append("\n");
                } catch (Exception e) {
                    info.append("Could not get package info: ").append(e.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            info.append("Error retrieving IME info: ").append(e.getMessage()).append("\n");
        }

        addZipEntry(zos, "hooked_keyboard_info.txt", info.toString());
    }

    private void addSettingsDump(ZipOutputStream zos) throws IOException {
        try {
            BackupManager bm = new BackupManager(context);
            // reused log, createBackup() already dumps everything except API keys
            String json = bm.createBackup();
            addZipEntry(zos, "settings_backup.json", json);
        } catch (Exception e) {
            addZipEntry(zos, "settings_backup_error.txt", "Error dumping settings: " + e.getMessage());
        }
    }

    private void addZipEntry(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] { "sh", "-c", command });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    private String executeRootCommand(String command) {
        if (!hasRootAccess) {
            return executeCommand(command);
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return "Error executing root command: " + e.getMessage();
        }
    }

    public static class ExportResult {
        public boolean success = false;
        public boolean hasRootAccess = false;
        public boolean deviceInfo = false;
        public boolean xposedLogs = false;
        public boolean moduleLogs = false;
        public boolean bootLogs = false;
        public boolean systemLogs = false;
        public boolean crashLogs = false;
        public boolean hookedKeyboardInfo = false;
        public boolean settingsDump = false;
        public String errorMessage = null;
    }
}
