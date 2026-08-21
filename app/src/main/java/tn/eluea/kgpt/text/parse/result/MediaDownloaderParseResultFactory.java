/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class MediaDownloaderParseResultFactory implements ParseResultFactory {
    public MediaDownloaderParseResultFactory() {
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        return new MediaDownloaderParseResult(groups, indexStart, indexEnd);
    }
}
