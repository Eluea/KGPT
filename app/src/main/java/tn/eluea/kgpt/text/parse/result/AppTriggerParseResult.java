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
package tn.eluea.kgpt.text.parse.result;

import java.util.List;

/**
 * Parse result for app trigger - opens an app when trigger word is typed
 */
public class AppTriggerParseResult extends ParseResult {
    public final String trigger;
    public final String packageName;
    public final String activityName;  // Activity class name for ComponentName launch
    public final String appName;

    public AppTriggerParseResult(List<String> groups, int indexStart, int indexEnd, 
                                  String trigger, String packageName, String activityName, String appName) {
        super(groups, indexStart, indexEnd);
        this.trigger = trigger;
        this.packageName = packageName;
        this.activityName = activityName;
        this.appName = appName;
    }
}
