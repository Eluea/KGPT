/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class MediaDownloaderParseResult extends ParseResult {
    public final String url;

    protected MediaDownloaderParseResult(List<String> groups, int indexStart, int indexEnd) {
        super(groups, indexStart, indexEnd);
        this.url = groups.get(1);
    }
}
