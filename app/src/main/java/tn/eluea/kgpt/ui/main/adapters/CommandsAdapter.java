/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.ui.main.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.instruction.command.InlineAskCommand;

public class CommandsAdapter extends RecyclerView.Adapter<CommandsAdapter.CommandViewHolder> {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";

    private List<GenerativeAICommand> commands;
    private OnCommandClickListener listener;
    private boolean showBuiltInCommands = true;
    private boolean isAmoledMode = false;

    public interface OnCommandClickListener {
        void onCommandClick(GenerativeAICommand command, int position);
    }

    public CommandsAdapter(List<GenerativeAICommand> commands, OnCommandClickListener listener) {
        this.commands = commands;
        this.listener = listener;
    }

    public void setAmoledMode(boolean isAmoled) {
        this.isAmoledMode = isAmoled;
    }

    @NonNull
    @Override
    public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Check AMOLED mode from preferences
        Context context = parent.getContext();
        SharedPreferences prefs = context.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);
        isAmoledMode = isDarkMode && prefs.getBoolean(PREF_AMOLED, false);

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command, parent, false);
        return new CommandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {
        GenerativeAICommand command = getCommandAtPosition(position);
        holder.bind(command, position);
    }

    @Override
    public int getItemCount() {
        int count = commands != null ? commands.size() : 0;
        if (showBuiltInCommands) {
            count += 1; // Add InlineAskCommand
        }
        return count;
    }

    private GenerativeAICommand getCommandAtPosition(int position) {
        if (showBuiltInCommands) {
            if (position == 0) {
                return InlineAskCommand.getInstance();
            }
            return commands.get(position - 1);
        }
        return commands.get(position);
    }

    private int getActualPosition(int adapterPosition) {
        if (showBuiltInCommands) {
            return adapterPosition - 1;
        }
        return adapterPosition;
    }

    public void updateCommands(List<GenerativeAICommand> newCommands) {
        this.commands = newCommands;
        notifyDataSetChanged();
    }

    class CommandViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCommandName;
        private TextView tvCommandDescription;
        private TextView tvCommandExample;
        private View itemContainer;

        CommandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommandName = itemView.findViewById(R.id.tv_command_name);
            tvCommandDescription = itemView.findViewById(R.id.tv_command_description);
            tvCommandExample = itemView.findViewById(R.id.tv_command_example);
            itemContainer = itemView;
        }

        void bind(GenerativeAICommand command, int position) {
            boolean isBuiltIn = InlineAskCommand.isInlineAskCommand(command);

            tvCommandName.setText("/" + command.getCommandPrefix());

            if (isBuiltIn) {
                // Built-in command styling
                Context ctx = itemView.getContext();
                tvCommandDescription.setText(ctx.getString(R.string.ask_command_description));
                tvCommandDescription.setVisibility(View.VISIBLE);

                // Add "BUILT-IN" indicator
                tvCommandName.setText("/" + command.getCommandPrefix() + ctx.getString(R.string.built_in_indicator));

                // Show special example for /ask
                if (tvCommandExample != null) {
                    tvCommandExample.setText(ctx.getString(R.string.ask_command_example));
                    tvCommandExample.setVisibility(View.VISIBLE);
                    // Apply AMOLED background if needed
                    if (isAmoledMode) {
                        tvCommandExample.setBackgroundResource(R.drawable.bg_example_chip_amoled);
                    }
                }

                // Allow editing built-in command (to change prefix)
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCommandClick(command, -1); // -1 indicates built-in
                    }
                });
            } else {
                String tweakMsg = command.getTweakMessage();
                if (tweakMsg != null && !tweakMsg.isEmpty()) {
                    tvCommandDescription.setText(tweakMsg);
                    tvCommandDescription.setVisibility(View.VISIBLE);
                } else {
                    tvCommandDescription.setText("Custom command");
                    tvCommandDescription.setVisibility(View.VISIBLE);
                }

                // Show custom example for user commands
                if (tvCommandExample != null) {
                    String example = getExampleForCommand(command);
                    tvCommandExample.setText("e.g. " + example);
                    tvCommandExample.setVisibility(View.VISIBLE);
                    // Apply AMOLED background if needed
                    if (isAmoledMode) {
                        tvCommandExample.setBackgroundResource(R.drawable.bg_example_chip_amoled);
                    }
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCommandClick(command, getActualPosition(position));
                    }
                });
            }
        }

        /**
         * Get custom example for each command
         */
        private String getExampleForCommand(GenerativeAICommand command) {
            String prefix = command.getCommandPrefix().toLowerCase();

            // Generate contextual examples based on command name - using /command$ format
            // The $ at the end is the AI trigger symbol (default)
            if (prefix.contains("translate")) {
                return "Hello world /" + command.getCommandPrefix() + "$";
            } else if (prefix.contains("fix") || prefix.contains("grammar")) {
                return "I has a apple /" + command.getCommandPrefix() + "$";
            } else if (prefix.contains("summar") || prefix.contains("short")) {
                return "[long text] /" + command.getCommandPrefix() + "$";
            } else if (prefix.contains("explain")) {
                return "Quantum physics /" + command.getCommandPrefix() + "$";
            } else if (prefix.contains("code") || prefix.contains("program")) {
                return "sort array /" + command.getCommandPrefix() + "$";
            } else if (prefix.contains("email") || prefix.contains("formal")) {
                return "meeting tomorrow /" + command.getCommandPrefix() + "$";
            } else {
                return "your text /" + command.getCommandPrefix() + "$";
            }
        }
    }
}
