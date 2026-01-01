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

public class WebSearchCommand extends AbstractCommand {
    @Override
    public String getCommandPrefix() {
        return "s";
    }
//    @Override
//    public void consume(String text, UiInteractor interacter, GenerativeAIController aiController) {
//        String url = "https://www.google.com/search?q=" + text;
//        interacter.showWebSearchDialog("Web Search", url);
//    }
}
