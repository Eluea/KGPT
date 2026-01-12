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
package tn.eluea.kgpt.llm;

import android.text.InputType;

public enum LanguageModelField {
    ApiKey("api_key", "Api Key", Type.String,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false),
    SubModel("sub_model", "Sub Model", Type.String,
            InputType.TYPE_CLASS_TEXT, false),
    BaseUrl("base_url", "Base Url", Type.String,
            InputType.TYPE_CLASS_TEXT, false),
    MaxTokens("max_tokens", "Max Tokens", Type.Integer,
            InputType.TYPE_CLASS_NUMBER, true),
    Temperature("temperature", "Temperature", Type.Double,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, true),
    TopP("top_p", "Top P", Type.Double,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, true);

    public final String name; // TODO update edittext accordingly, record demo videos, update readme.md
    public final String title;
    public final Type type;
    public final int inputType;
    public final boolean advanced;

    LanguageModelField(String name, String title, Type type, int inputType, boolean advanced) {
        this.name = name;
        this.title = title;
        this.type = type;
        this.inputType = inputType;
        this.advanced = advanced;
    }

    public enum Type {
        String,
        Integer,
        Double
    }
}
