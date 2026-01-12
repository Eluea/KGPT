package tn.eluea.kgpt.ui.main.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.instruction.command.InlineAskCommand;
import tn.eluea.kgpt.instruction.command.SimpleGenerativeAICommand;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.main.adapters.CommandsAdapter;
import tn.eluea.kgpt.ui.main.fragments.AiInvocationFragment;

public class InvocationCommandsFragment extends Fragment {

    private RecyclerView rvCommands;
    private CommandsAdapter commandsAdapter;
    private List<GenerativeAICommand> commands = new ArrayList<>();
    private View emptyState;

    // Config fields
    private static final String PREF_INLINE_ASK_PREFIX = "inline_ask_prefix";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invocation_commands, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvCommands = view.findViewById(R.id.rv_commands);
        emptyState = view.findViewById(R.id.empty_state);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        rvCommands.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadCommands();

        commandsAdapter = new CommandsAdapter(commands, new CommandsAdapter.OnCommandClickListener() {
            @Override
            public void onCommandClick(GenerativeAICommand command, int position) {
                if (position == -1) {
                    showEditInlineAskDialog();
                } else {
                    showEditCommandDialog(command, position);
                }
            }
        });
        rvCommands.setAdapter(commandsAdapter);
        updateEmptyState();
    }

    public void loadCommands() {
        if (SPManager.isReady()) {
            commands = new ArrayList<>(SPManager.getInstance().getGenerativeAICommands());

            // Reload inline ask prefix
            String savedPrefix = SPManager.getInstance().getConfigClient().getString(PREF_INLINE_ASK_PREFIX,
                    InlineAskCommand.DEFAULT_PREFIX);
            InlineAskCommand.setPrefix(savedPrefix);

            if (commandsAdapter != null) {
                commandsAdapter.updateCommands(commands);
            }
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (commands.isEmpty() && emptyState != null) {
            // Check if we count inline ask. InlineAsk is always present in adapter but not
            // in list.
            // Adapter adds +1 for InlineAsk. So it's never truly empty for the user.
            // So empty state might only be relevant if we didn't have built-in commands.
            emptyState.setVisibility(View.GONE);
        } else if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    public void showAddCommandDialog() {
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

            for (GenerativeAICommand cmd : commands) {
                if (cmd.getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (InlineAskCommand.isInlineAskCommand(commandName) || commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleGenerativeAICommand newCommand = new SimpleGenerativeAICommand(commandName, systemMessage);
            commands.add(newCommand);
            saveCommands();
            commandsAdapter.updateCommands(commands);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Command added", Toast.LENGTH_SHORT).show();
        });

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showEditCommandDialog(GenerativeAICommand command, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null)
            tvTitle.setText("Edit Command");

        EditText etCommandName = dialogView.findViewById(R.id.et_command_name);
        EditText etSystemMessage = dialogView.findViewById(R.id.et_system_message);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btn_delete);

        etCommandName.setText(command.getCommandPrefix());
        etSystemMessage.setText(command.getTweakMessage());

        // Show delete button in edit mode
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteCommandConfirmation(command, position);
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String commandName = etCommandName.getText().toString().trim();
            String systemMessage = etSystemMessage.getText().toString().trim();

            if (commandName.isEmpty()) {
                Toast.makeText(requireContext(), "Command name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < commands.size(); i++) {
                if (i != position && commands.get(i).getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (InlineAskCommand.isInlineAskCommand(commandName) || commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            commands.set(position, new SimpleGenerativeAICommand(commandName, systemMessage));
            saveCommands();
            commandsAdapter.updateCommands(commands);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Command updated", Toast.LENGTH_SHORT).show();
        });

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showEditInlineAskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null)
            tvTitle.setText("Edit Inline Ask Command");

        EditText etCommandName = dialogView.findViewById(R.id.et_command_name);
        EditText etSystemMessage = dialogView.findViewById(R.id.et_system_message);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // Hide system message
        if (etSystemMessage != null && etSystemMessage.getParent() != null) {
            View parent = (View) etSystemMessage.getParent().getParent();
            if (parent != null)
                parent.setVisibility(View.GONE);
        }

        etCommandName.setText(InlineAskCommand.getPrefix());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String commandName = etCommandName.getText().toString().trim();

            if (commandName.isEmpty()) {
                Toast.makeText(requireContext(), "Command name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            for (GenerativeAICommand cmd : commands) {
                if (cmd.getCommandPrefix().equalsIgnoreCase(commandName)) {
                    Toast.makeText(requireContext(), "Command already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (commandName.equalsIgnoreCase("s")) {
                Toast.makeText(requireContext(), "Cannot use built-in command name", Toast.LENGTH_SHORT).show();
                return;
            }

            InlineAskCommand.setPrefix(commandName);
            if (SPManager.isReady()) {
                SPManager.getInstance().getConfigClient().putString(PREF_INLINE_ASK_PREFIX, commandName);
            }

            commandsAdapter.notifyDataSetChanged();
            syncConfig();
            dialog.dismiss();
            Toast.makeText(requireContext(), "Inline Ask command updated", Toast.LENGTH_SHORT).show();
        });

        BottomSheetHelper.applyBlur(dialog);
        dialog.show();
    }

    private void showDeleteCommandConfirmation(GenerativeAICommand command, int position) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_delete_confirm, null);
        BottomSheetHelper.applyTheme(requireContext(), sheetView);
        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        android.widget.TextView tvTitle = sheetView.findViewById(R.id.tv_delete_title);
        android.widget.TextView tvMessage = sheetView.findViewById(R.id.tv_delete_message);
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

    private void saveCommands() {
        if (SPManager.isReady()) {
            SPManager.getInstance().setGenerativeAICommands(commands);
            syncConfig();
        }
    }

    private void syncConfig() {
        Intent intent = new Intent("tn.eluea.kgpt.DIALOG_RESULT");
        String commandsRaw = SPManager.getInstance().getGenerativeAICommandsRaw();
        intent.putExtra("tn.eluea.kgpt.command.LIST", commandsRaw);
        // We also need patterns to be safe, but since we are split, maybe just sending
        // commands is enough
        // if the receiver handles partial updates?
        // Looking at previous AiInvocationFragment, it sent both.
        // Let's grab patterns from SPManager as well just in case.
        String patternsRaw = SPManager.getInstance().getParsePatternsRaw();
        intent.putExtra("tn.eluea.kgpt.pattern.LIST", patternsRaw);
        requireContext().sendBroadcast(intent);
    }
}
