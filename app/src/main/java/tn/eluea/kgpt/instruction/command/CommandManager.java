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

import android.os.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.llm.LanguageModel;

public class CommandManager implements ConfigChangeListener {
    private final static Map<String, AbstractCommand> STATIC_COMMAND_MAP = Map.of(
            "s", new WebSearchCommand()
    );

    private Map<String, AbstractCommand> commandMap;

    public CommandManager() {
        UiInteractor.getInstance().registerConfigChangeListener(this);

        List<GenerativeAICommand> aiCommands = SPManager.getInstance().getGenerativeAICommands();
        updateCommandMap(aiCommands);
    }

    private void updateCommandMap(List<GenerativeAICommand> aiCommands) {
        commandMap = new HashMap<>(STATIC_COMMAND_MAP);
        // Add InlineAskCommand with its current prefix
        commandMap.put(InlineAskCommand.getPrefix(), InlineAskCommand.getInstance());
        
        for (GenerativeAICommand command: aiCommands) {
            // Don't allow overriding built-in commands
            if (!isBuiltInCommand(command.getCommandPrefix())) {
                commandMap.put(command.getCommandPrefix(), command);
            }
        }
    }

    public AbstractCommand get(String prefix) {
        return commandMap.get(prefix);
    }
    
    /**
     * Check if a command prefix is a built-in command that cannot be deleted
     */
    public static boolean isBuiltInCommand(String prefix) {
        return STATIC_COMMAND_MAP.containsKey(prefix) || 
               InlineAskCommand.isInlineAskCommand(prefix);
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {

    }

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {

    }

    @Override
    public void onCommandsChange(String commandsRaw) {
        updateCommandMap(Commands.decodeCommands(commandsRaw));
    }

    @Override
    public void onPatternsChange(String patternsRaw) {

    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {

    }
}
