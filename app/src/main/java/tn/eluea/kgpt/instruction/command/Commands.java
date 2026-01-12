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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Commands {

    /**
     * Default commands that come pre-installed with the app
     */
    public static ArrayList<GenerativeAICommand> getDefaultCommands() {
        ArrayList<GenerativeAICommand> defaults = new ArrayList<>();
        
        defaults.add(new SimpleGenerativeAICommand("tr", 
            "Translate the following text. If it's in Arabic, translate to English and vice versa. Give only the translation."));
        
        defaults.add(new SimpleGenerativeAICommand("fix", 
            "Fix spelling and grammar errors in the following text. Give only the corrected text."));
        
        defaults.add(new SimpleGenerativeAICommand("short", 
            "Summarize the following text in one or two sentences."));
        
        defaults.add(new SimpleGenerativeAICommand("formal", 
            "Rewrite the following text in a formal and professional style. Give only the rewritten text."));
        
        defaults.add(new SimpleGenerativeAICommand("casual", 
            "Rewrite the following text in a friendly and casual style. Give only the rewritten text."));
        
        defaults.add(new SimpleGenerativeAICommand("reply", 
            "Write a short and appropriate reply to the following message. Give only the reply."));
        
        defaults.add(new SimpleGenerativeAICommand("email", 
            "Write a professional email about the following topic."));
        
        defaults.add(new SimpleGenerativeAICommand("explain", 
            "Explain the following topic in a simple and easy to understand way."));
        
        defaults.add(new SimpleGenerativeAICommand("code", 
            "Write the requested code without additional explanation. Give only the code."));
        
        defaults.add(new SimpleGenerativeAICommand("emoji", 
            "Add appropriate emojis to the following text. Give only the text with emojis."));
        
        return defaults;
    }

    public static String encodeCommands(List<GenerativeAICommand> commands) {
        JSONArray rootJson = new JSONArray();
        for (GenerativeAICommand command: commands) {
            try {
                rootJson.put(new JSONObject()
                        .accumulate("prefix", command.getCommandPrefix())
                        .accumulate("message", command.getTweakMessage()));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return rootJson.toString();
    }

    public static ArrayList<GenerativeAICommand> decodeCommands(String rawCommands) {
        ArrayList<GenerativeAICommand> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(rawCommands);
            for (int i=0; i<array.length(); i++) {
                JSONObject commandJson = (JSONObject) array.get(i);
                String prefix = commandJson.getString("prefix");
                String message = commandJson.getString("message");
                result.add(new SimpleGenerativeAICommand(prefix, message));
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
