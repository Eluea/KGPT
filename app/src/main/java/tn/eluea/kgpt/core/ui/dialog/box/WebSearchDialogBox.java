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
package tn.eluea.kgpt.core.ui.dialog.box;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.features.websearch.WebSearchActivity;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.ui.UiInteractor;

public class WebSearchDialogBox extends DialogBox {
    public WebSearchDialogBox(DialogBoxManager dialogManager, Activity parent,
                              Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        String url = getInput().getString(UiInteractor.EXTRA_WEBVIEW_URL);
        String title = getInput().getString(UiInteractor.EXTRA_WEBVIEW_TITLE);
        String searchEngine = getInput().getString(UiInteractor.EXTRA_SEARCH_ENGINE, "duckduckgo");
        
        if (url == null) {
            throw new NullPointerException(UiInteractor.EXTRA_WEBVIEW_URL + " cannot be null");
        }

        // Launch WebSearchActivity as floating bottom sheet
        Intent intent = new Intent(getContext(), WebSearchActivity.class);
        intent.putExtra(UiInteractor.EXTRA_WEBVIEW_URL, url);
        intent.putExtra(UiInteractor.EXTRA_WEBVIEW_TITLE, title);
        intent.putExtra(UiInteractor.EXTRA_SEARCH_ENGINE, searchEngine);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        
        // Finish the DialogActivity
        getParent().finish();
        
        // Return null since we're using a separate activity
        return null;
    }
}
