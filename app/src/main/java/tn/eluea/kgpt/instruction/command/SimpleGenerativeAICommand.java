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
package tn.eluea.kgpt.instruction.command;

public class SimpleGenerativeAICommand extends GenerativeAICommand {
    private final String mPrefix;
    private final String mTweakMessage;

    public SimpleGenerativeAICommand(String prefix, String tweakMessage) {
        mPrefix = prefix;
        mTweakMessage = tweakMessage;
    }

    @Override
    public String getCommandPrefix() {
        return mPrefix;
    }

    @Override
    public String getTweakMessage() {
        return mTweakMessage;
    }
}
