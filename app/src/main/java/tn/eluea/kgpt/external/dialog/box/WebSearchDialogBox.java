package tn.eluea.kgpt.external.dialog.box;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.WebSearchActivity;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
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
