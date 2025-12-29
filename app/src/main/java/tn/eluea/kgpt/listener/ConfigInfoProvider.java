package tn.eluea.kgpt.listener;

import android.os.Bundle;

import tn.eluea.kgpt.llm.LanguageModel;

public interface ConfigInfoProvider {
    LanguageModel getLanguageModel();

    Bundle getConfigBundle();

    Bundle getOtherSettings();
}
