package tn.eluea.kgpt;

import android.content.Context;

import tn.eluea.kgpt.instruction.command.AbstractCommand;
import tn.eluea.kgpt.instruction.command.CommandManager;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.instruction.command.WebSearchCommand;
import tn.eluea.kgpt.listener.DialogDismissListener;
import tn.eluea.kgpt.listener.GenerativeAIListener;
import tn.eluea.kgpt.listener.InputEventListener;
import tn.eluea.kgpt.llm.GenerativeAIController;
import tn.eluea.kgpt.text.TextParser;
import tn.eluea.kgpt.text.parse.result.AIParseResult;
import tn.eluea.kgpt.text.parse.result.CommandParseResult;
import tn.eluea.kgpt.text.parse.result.FormatParseResult;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResult;
import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.text.parse.result.SettingsParseResult;
import tn.eluea.kgpt.text.parse.result.WebSearchParseResult;
import tn.eluea.kgpt.text.transform.format.TextUnicodeConverter;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;

public class KeyboardGPTBrain implements InputEventListener, GenerativeAIListener, DialogDismissListener {
    private final static String STR_GENERATING_CONTENT = "<Generating Content...>";
    private boolean justPrepared = true;

    private final GenerativeAIController mAIController;
    private final CommandManager mCommandManager;
    private final TextParser mTextParser;
    private final SPUpdater mSPUpdater;

    public KeyboardGPTBrain(Context context) {
        IMSController.getInstance().addListener(this);
        UiInteractor.getInstance().registerOnDismissListener(this);

        mAIController = new GenerativeAIController();
        mAIController.addListener(this);
        mCommandManager = new CommandManager();
        mTextParser = new TextParser();
        mSPUpdater = new SPUpdater();
    }

    @Override
    public void onTextUpdate(String text, int cursor) {
        IMSController imsController = UiInteractor.getInstance().getIMSController();
        ParseResult result = mTextParser.parse(text, cursor);
        if (result != null) {
            if (result.indexEnd == cursor) {
                int deleteCount = result.indexEnd - result.indexStart;

                imsController.stopNotifyInput();
                imsController.delete(deleteCount);
                imsController.startNotifyInput();

                processParsedText(text, result);
            }
        }
    }

    public void processParsedText(String text, ParseResult parseResult) {
        IMSController imsController = UiInteractor.getInstance().getIMSController();
        if (parseResult instanceof FormatParseResult) {
            FormatParseResult formatParseResult = (FormatParseResult) parseResult;
            String newText = TextUnicodeConverter.convert(formatParseResult.target, formatParseResult.conversionMethod);

            imsController.stopNotifyInput();
            imsController.commit(newText);
            imsController.startNotifyInput();
        } else if (parseResult instanceof AIParseResult) {
            AIParseResult aiParseResult = (AIParseResult) parseResult;
            generateResponse(aiParseResult.prompt, null);
        } else if (parseResult instanceof InlineAskParseResult) {
            InlineAskParseResult inlineAskResult = (InlineAskParseResult) parseResult;
            generateResponse(inlineAskResult.prompt, null);
        } else if (parseResult instanceof CommandParseResult) {
            CommandParseResult commandParseResult = (CommandParseResult) parseResult;
            if (commandParseResult.command.isEmpty()) {
                UiInteractor.getInstance().showEditCommandsDialog();
            } else {
                AbstractCommand command = mCommandManager.get(commandParseResult.command);
                if (command instanceof GenerativeAICommand) {
                    GenerativeAICommand genAICommand = (GenerativeAICommand) command;
                    generateResponse(commandParseResult.prompt, genAICommand.getTweakMessage());
                } else if (command instanceof WebSearchCommand){
                    String url = "https://duckduckgo.com/?q=" + commandParseResult.prompt;
                    UiInteractor.getInstance().showWebSearchDialog("Web Search", url);
                }
            }
        } else if (parseResult instanceof SettingsParseResult) {
            UiInteractor.getInstance().showSettingsDialog();
        } else if (parseResult instanceof WebSearchParseResult) {
            WebSearchParseResult webSearchResult = (WebSearchParseResult) parseResult;
            Context context = MainHook.getApplicationContext();
            String url = SPManager.getSearchUrlFromKGPT(context, webSearchResult.query);
            UiInteractor.getInstance().showWebSearchDialog("Web Search", url);
        }
    }

    private void generateResponse(String prompt, String systemMessage) {
        if (prompt.isEmpty() || mAIController.needModelClient()) {
            if (UiInteractor.getInstance().showChoseModelDialog()) {
                UiInteractor.getInstance().toastLong("Chose and configure your language model");
            }
            return;
        }

        if (mAIController.needApiKey()) {
            if (UiInteractor.getInstance().showChoseModelDialog()) {
                UiInteractor.getInstance().toastLong(mAIController.getLanguageModel().label +
                        " is Missing API Key");
            }
            return;
        }

        new Thread(() -> mAIController.generateResponse(prompt, systemMessage)).start();
    }

    @Override
    public void onAIPrepare() {
        IMSController.getInstance().flush();
        IMSController.getInstance().commit(STR_GENERATING_CONTENT);
        IMSController.getInstance().stopNotifyInput();
        IMSController.getInstance().startInputLock();
        justPrepared = true;
    }

    private void clearGeneratingContent() {
        if (justPrepared) {
            justPrepared = false;
            IMSController.getInstance().flush();
            IMSController.getInstance().delete(STR_GENERATING_CONTENT.length());
        }
    }

    @Override
    public void onAINext(String chunk) {
        IMSController.getInstance().endInputLock();
        clearGeneratingContent();
        IMSController.getInstance().flush();
        IMSController.getInstance().commit(chunk);
        IMSController.getInstance().startInputLock();
    }

    @Override
    public void onAIError(Throwable t) {
        IMSController.getInstance().endInputLock();
        clearGeneratingContent();
        
        String errorMsg = t.getMessage();
        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = "Unknown error occurred";
        }
        
        String displayError = "[Error: " + errorMsg + "]";
        IMSController.getInstance().flush();
        IMSController.getInstance().commit(displayError);
        IMSController.getInstance().startNotifyInput();
    }

    @Override
    public void onAIComplete() {
        IMSController.getInstance().endInputLock();
        clearGeneratingContent();
        IMSController.getInstance().startNotifyInput();
    }

    @Override
    public void onDismiss(boolean isPrompt, boolean isCommand, boolean isPattern) {
        if (isPrompt) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("Selected " + mAIController.getLanguageModel()
                        + " (" + mAIController.getModelClient().getSubModel() + ")");
            });
        } else if (isCommand) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("New Commands Saved");
            });
        } else if (isPattern) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("New Pattern Saved");
            });
        }
    }
}
