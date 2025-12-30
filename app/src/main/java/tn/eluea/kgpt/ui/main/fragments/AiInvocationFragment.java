package tn.eluea.kgpt.ui.main.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.instruction.command.InlineAskCommand;
import tn.eluea.kgpt.instruction.command.SimpleGenerativeAICommand;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.main.adapters.CommandsAdapter;
import tn.eluea.kgpt.ui.main.adapters.PatternsAdapter;

public class AiInvocationFragment extends Fragment {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";
    private static final String ACTION_DIALOG_RESULT = "tn.eluea.kgpt.DIALOG_RESULT";
    private static final String EXTRA_PATTERN_LIST = "tn.eluea.kgpt.pattern.LIST";
    private static final String EXTRA_COMMAND_LIST = "tn.eluea.kgpt.command.LIST";
    private static final String PREF_INLINE_ASK_PREFIX = "inline_ask_prefix";
    
    // Maximum limits
    private static final int MAX_TRIGGERS = 3;

    private RecyclerView rvCommands, rvPatterns;
    private LinearLayout btnAddCommand, btnAddPattern;
    private FrameLayout btnInfo;
    private View rootLayout;

    private CommandsAdapter commandsAdapter;
    private PatternsAdapter patternsAdapter;
    private List<GenerativeAICommand> commands = new ArrayList<>();
    private List<ParsePattern> patterns = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_invocation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootLayout = view.findViewById(R.id.root_layout);
        initViews(view);
        applyAmoledIfNeeded();
        setupRecyclerViews();
        setupButtons();
    }

    private void initViews(View view) {
        rvCommands = view.findViewById(R.id.rv_commands);
        rvPatterns = view.findViewById(R.id.rv_patterns);
        btnAddCommand = view.findViewById(R.id.btn_add_command);
        btnAddPattern = view.findViewById(R.id.btn_add_pattern);
        btnInfo = view.findViewById(R.id.btn_info);
    }

    private void applyAmoledIfNeeded() {
        SharedPreferences prefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = prefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);

        if (isDarkMode && isAmoled) {
            rootLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
            applyAmoledToCards(rootLayout);
        }
    }

    private void applyAmoledToCards(View view) {
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            // Check if this is a container card or item card based on corner radius
            float cornerRadius = card.getRadius();
            if (cornerRadius >= 18) {
                // Container card (20dp radius)
                card.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.container_background_amoled)
                );
            } else {
                // Item card (12dp radius)
                card.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.item_card_background_amoled)
                );
                card.setStrokeColor(
                    ContextCompat.getColor(requireContext(), R.color.item_card_border_amoled)
                );
            }
            // Apply AMOLED to children of card (iterate children directly)
            for (int i = 0; i < card.getChildCount(); i++) {
                applyAmoledToCards(card.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            // Check if this is an example chip (has bg_example_chip background)
            if (tv.getBackground() != null && tv.getId() == R.id.tv_command_example) {
                tv.setBackgroundResource(R.drawable.bg_example_chip_amoled);
            } else if (tv.getBackground() != null && tv.getId() == R.id.tv_pattern_example) {
                tv.setBackgroundResource(R.drawable.bg_example_chip_amoled);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    private void setupRecyclerViews() {
        rvCommands.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPatterns.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load commands and patterns
        if (SPManager.isReady()) {
            commands = new ArrayList<>(SPManager.getInstance().getGenerativeAICommands());
            patterns = new ArrayList<>(SPManager.getInstance().getParsePatterns());
            
            // Load saved inline ask prefix from ConfigProvider (world-readable)
            String savedPrefix = SPManager.getInstance().getConfigClient().getString(PREF_INLINE_ASK_PREFIX, InlineAskCommand.DEFAULT_PREFIX);
            InlineAskCommand.setPrefix(savedPrefix);
        }

        // Setup Commands Adapter
        commandsAdapter = new CommandsAdapter(commands, new CommandsAdapter.OnCommandClickListener() {
            @Override
            public void onCommandClick(GenerativeAICommand command, int position) {
                if (position == -1) {
                    // Built-in command (InlineAsk)
                    showEditInlineAskDialog();
                } else {
                    showEditCommandDialog(command, position);
                }
            }

            @Override
            public void onCommandDelete(GenerativeAICommand command, int position) {
                showDeleteCommandConfirmation(command, position);
            }
        });
        rvCommands.setAdapter(commandsAdapter);

        // Setup Patterns Adapter
        patternsAdapter = new PatternsAdapter(patterns, new PatternsAdapter.OnPatternClickListener() {
            @Override
            public void onPatternClick(ParsePattern pattern, int position) {
                showEditPatternDialog(pattern, position);
            }

            @Override
            public void onPatternDelete(ParsePattern pattern, int position) {
                showResetPatternConfirmation(pattern, position);
            }
        });
        rvPatterns.setAdapter(patternsAdapter);
    }

    private void setupButtons() {
        btnAddCommand.setOnClickListener(v -> showAddCommandDialog());
        btnAddPattern.setOnClickListener(v -> showInfoBottomSheet());
        btnInfo.setOnClickListener(v -> showInfoBottomSheet());
    }

    private void showAddCommandDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etCommandName = dialogView.findViewById(R.id.et_command_name);
        EditText etSystemMessage = dialogView.findViewById(R.id.et_system_message);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String commandName = etCommandName.getText().toString().trim();
            String systemMessage = etSystemMessage.getText().toString().trim();

            if (commandName.isEmpty()) {
                Toast.makeText(requireContext(), "Command name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate command
            for (GenerativeAICommand cmd : commands) {
                if (cmd.getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Check for built-in command names
            if (InlineAskCommand.isInlineAskCommand(commandName) || commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add new command
            SimpleGenerativeAICommand newCommand = new SimpleGenerativeAICommand(commandName, systemMessage);
            commands.add(newCommand);
            saveCommands();
            commandsAdapter.updateCommands(commands);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Command added", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditCommandDialog(GenerativeAICommand command, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Update title
        TextView tvTitle = dialogView.findViewById(android.R.id.text1);
        if (tvTitle != null) {
            tvTitle.setText("Edit Command");
        }

        EditText etCommandName = dialogView.findViewById(R.id.et_command_name);
        EditText etSystemMessage = dialogView.findViewById(R.id.et_system_message);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // Pre-fill values
        etCommandName.setText(command.getCommandPrefix());
        etSystemMessage.setText(command.getTweakMessage());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String commandName = etCommandName.getText().toString().trim();
            String systemMessage = etSystemMessage.getText().toString().trim();

            if (commandName.isEmpty()) {
                Toast.makeText(requireContext(), "Command name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate (excluding current)
            for (int i = 0; i < commands.size(); i++) {
                if (i != position && commands.get(i).getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Check for built-in command names
            if (InlineAskCommand.isInlineAskCommand(commandName) || commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update command
            commands.set(position, new SimpleGenerativeAICommand(commandName, systemMessage));
            saveCommands();
            commandsAdapter.updateCommands(commands);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Command updated", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    
    private void showEditInlineAskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Update title
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) {
            tvTitle.setText("Edit Inline Ask Command");
        }

        EditText etCommandName = dialogView.findViewById(R.id.et_command_name);
        EditText etSystemMessage = dialogView.findViewById(R.id.et_system_message);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        
        // Hide system message for built-in command
        // Find the parent TextInputLayout of etSystemMessage
        if (etSystemMessage != null && etSystemMessage.getParent() != null) {
            View parent = (View) etSystemMessage.getParent().getParent();
            if (parent != null) {
                parent.setVisibility(View.GONE);
            }
        }

        // Pre-fill current prefix
        etCommandName.setText(InlineAskCommand.getPrefix());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String commandName = etCommandName.getText().toString().trim();

            if (commandName.isEmpty()) {
                Toast.makeText(requireContext(), "Command name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate with user commands
            for (GenerativeAICommand cmd : commands) {
                if (cmd.getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Check for other built-in command names
            if (commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update inline ask prefix
            InlineAskCommand.setPrefix(commandName);
            
            // Save to ConfigProvider (world-readable) instead of private preferences
            // This ensures the Xposed module can read the setting
            if (SPManager.isReady()) {
                SPManager.getInstance().getConfigClient().putString(PREF_INLINE_ASK_PREFIX, commandName);
            }
            
            commandsAdapter.notifyDataSetChanged();
            syncConfig();
            dialog.dismiss();
            Toast.makeText(requireContext(), "Inline Ask command updated to /" + commandName, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDeleteCommandConfirmation(GenerativeAICommand command, int position) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_delete_confirm, null);
        
        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tv_delete_title);
        TextView tvMessage = sheetView.findViewById(R.id.tv_delete_message);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);
        MaterialButton btnDelete = sheetView.findViewById(R.id.btn_delete);

        tvTitle.setText("Delete Command");
        tvMessage.setText("Are you sure you want to delete '/" + command.getCommandPrefix() + "'?");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            commands.remove(position);
            saveCommands();
            commandsAdapter.updateCommands(commands);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Command deleted", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditPatternDialog(ParsePattern pattern, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_pattern_symbol, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvPatternType = dialogView.findViewById(R.id.tv_pattern_type);
        TextView tvDescription = dialogView.findViewById(R.id.tv_description);
        TextInputEditText etSymbol = dialogView.findViewById(R.id.et_symbol);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.input_layout_symbol);
        TextView tvExample = dialogView.findViewById(R.id.tv_example);
        com.google.android.material.materialswitch.MaterialSwitch switchEnabled = dialogView.findViewById(R.id.switch_enabled);
        MaterialButton btnReset = dialogView.findViewById(R.id.btn_reset);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // Set values
        tvPatternType.setText(pattern.getType().title);
        tvDescription.setText(pattern.getType().description);
        
        // Set enabled state
        switchEnabled.setChecked(pattern.isEnabled());
        
        // Extract current symbol from regex
        String currentSymbol = PatternType.regexToSymbol(pattern.getPattern().pattern());
        if (currentSymbol == null || currentSymbol.isEmpty()) {
            currentSymbol = pattern.getType().defaultSymbol;
        }
        etSymbol.setText(currentSymbol);
        
        // Update example based on symbol
        updateExample(tvExample, currentSymbol, pattern.getType());
        
        // Listen for symbol changes to update example
        etSymbol.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateExample(tvExample, s.toString(), pattern.getType());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnReset.setOnClickListener(v -> {
            etSymbol.setText(pattern.getType().defaultSymbol);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString();
            boolean isEnabled = switchEnabled.isChecked();

            if (symbol.isEmpty()) {
                Toast.makeText(requireContext(), "Symbol cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Forbidden symbols
            if (List.of("]", "[", "-", " ", "\n", "\t").contains(symbol)) {
                Toast.makeText(requireContext(), "This symbol is not allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert symbol to regex
            String newRegex = PatternType.symbolToRegex(symbol, pattern.getType().groupCount);
            if (newRegex == null) {
                Toast.makeText(requireContext(), "Could not create pattern for this symbol", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate regex
            try {
                java.util.regex.Pattern.compile(newRegex);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Invalid pattern generated", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update pattern with enabled state
            ParsePattern newPattern = new ParsePattern(pattern.getType(), newRegex, pattern.getExtras());
            newPattern.setEnabled(isEnabled);
            patterns.set(position, newPattern);
            savePatterns();
            patternsAdapter.updatePatterns(patterns);
            dialog.dismiss();
            
            String statusMsg = isEnabled ? "enabled" : "disabled";
            Toast.makeText(requireContext(), "Trigger \"" + symbol + "\" " + statusMsg, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    
    private void updateExample(TextView tvExample, String symbol, PatternType type) {
        if (symbol == null || symbol.isEmpty()) {
            symbol = type.defaultSymbol;
        }
        
        String example;
        switch (type) {
            case Settings:
                example = "Example: \"" + symbol + "\" → Opens settings";
                break;
            case CommandAI:
                example = "Example: \"Hello, how are you?" + symbol + "\" → AI responds";
                break;
            case CommandCustom:
                example = "Example: \"Hello" + symbol + "translate" + symbol + "\" → Translates";
                break;
            case FormatItalic:
                example = "Example: \"text" + symbol + "\" → italic text";
                break;
            case FormatBold:
                example = "Example: \"text" + symbol + "\" → bold text";
                break;
            case FormatCrossout:
                example = "Example: \"text" + symbol + "\" → strikethrough";
                break;
            case FormatUnderline:
                example = "Example: \"text" + symbol + "\" → underlined";
                break;
            default:
                example = "Type your text, then add \"" + symbol + "\" at the end";
        }
        tvExample.setText(example);
    }

    private void showResetPatternConfirmation(ParsePattern pattern, int position) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_delete_confirm, null);
        
        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tv_delete_title);
        TextView tvMessage = sheetView.findViewById(R.id.tv_delete_message);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);
        MaterialButton btnDelete = sheetView.findViewById(R.id.btn_delete);

        tvTitle.setText("Reset Pattern");
        tvMessage.setText("Reset '" + pattern.getType().title + "' to default symbol \"" + pattern.getType().defaultSymbol + "\"?");
        btnDelete.setText("Reset");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            patterns.set(position, new ParsePattern(pattern.getType(), pattern.getType().defaultPattern));
            savePatterns();
            patternsAdapter.updatePatterns(patterns);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Pattern reset to default", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void saveCommands() {
        if (SPManager.isReady()) {
            SPManager.getInstance().setGenerativeAICommands(commands);
            syncConfig();
        }
    }

    private void savePatterns() {
        if (SPManager.isReady()) {
            SPManager.getInstance().setParsePatterns(patterns);
            syncConfig();
        }
    }

    private void syncConfig() {
        // Send broadcast to sync with Xposed module using the same action it listens to
        Intent intent = new Intent(ACTION_DIALOG_RESULT);
        
        // Include patterns data (get raw from SPManager after saving)
        String patternsRaw = SPManager.getInstance().getParsePatternsRaw();
        intent.putExtra(EXTRA_PATTERN_LIST, patternsRaw);
        
        // Include commands data
        String commandsRaw = SPManager.getInstance().getGenerativeAICommandsRaw();
        intent.putExtra(EXTRA_COMMAND_LIST, commandsRaw);
        
        requireContext().sendBroadcast(intent);
    }

    private void showInfoBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_ai_usage, null);
        
        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        // Get current trigger symbols dynamically
        String aiTriggerSymbol = "$"; // default
        String italicSymbol = "|";
        String boldSymbol = "@";
        String crossoutSymbol = "~";
        String underlineSymbol = "_";
        
        for (ParsePattern pattern : patterns) {
            String symbol = PatternType.regexToSymbol(pattern.getPattern().pattern());
            if (symbol == null) symbol = pattern.getType().defaultSymbol;
            
            switch (pattern.getType()) {
                case CommandAI:
                    aiTriggerSymbol = symbol;
                    break;
                case FormatItalic:
                    italicSymbol = symbol;
                    break;
                case FormatBold:
                    boldSymbol = symbol;
                    break;
                case FormatCrossout:
                    crossoutSymbol = symbol;
                    break;
                case FormatUnderline:
                    underlineSymbol = symbol;
                    break;
            }
        }
        
        // Get first command name (if exists)
        String commandName = "translate";
        if (commands != null && !commands.isEmpty()) {
            commandName = commands.get(0).getCommandPrefix();
        }
        
        // Get /ask prefix
        String askPrefix = InlineAskCommand.getInstance().getCommandPrefix();
        
        // Update examples dynamically
        TextView tvAiExample = sheetView.findViewById(R.id.tv_ai_trigger_example);
        TextView tvAskExample = sheetView.findViewById(R.id.tv_ask_example);
        TextView tvCommandExample = sheetView.findViewById(R.id.tv_command_example);
        TextView tvFormatExample = sheetView.findViewById(R.id.tv_format_example);
        
        tvAiExample.setText("What is AI?" + aiTriggerSymbol);
        tvAskExample.setText("Note. /" + askPrefix + " time?" + aiTriggerSymbol + " → keeps Note.");
        tvCommandExample.setText("Hello /" + commandName + aiTriggerSymbol);
        tvFormatExample.setText("text" + italicSymbol + " text" + boldSymbol + " text" + crossoutSymbol + " text" + underlineSymbol);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
