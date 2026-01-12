package tn.eluea.kgpt.core.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.listener.GenerativeAIListener;
import tn.eluea.kgpt.llm.GenerativeAIController;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;

public class AiResponseManager implements GenerativeAIListener {

    private final GenerativeAIController mAIController;
    private final Runnable onAiPrepareCallback;
    private boolean justPrepared = true;

    // State for text actions (replace mode)
    private boolean isTextActionMode = false;
    private String pendingSelectedText = null;

    // Use method to get string to support locale changes and resources
    private String getGeneratingContentString() {
        Context ctx = UiInteractor.getInstance().getContext();
        if (ctx != null) {
            try {
                return ctx.getString(R.string.generating_content);
            } catch (Exception e) {
                // Fallback if resource not found (e.g. running in Xposed context with wrong
                // Resources)
                return "<Generating Content...>";
            }
        }
        return "<Generating Content...>";
    }

    // Thread pool for AI requests - reuse threads instead of creating new ones
    private static final ExecutorService aiExecutor = Executors.newFixedThreadPool(2);

    // Shutdown hook to clean up executor
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            aiExecutor.shutdown();
            try {
                if (!aiExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    aiExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                aiExecutor.shutdownNow();
            }
        }));
    }

    public AiResponseManager(GenerativeAIController aiController, Runnable onAiPrepareCallback) {
        this.mAIController = aiController;
        this.onAiPrepareCallback = onAiPrepareCallback;
        this.mAIController.addListener(this);
    }

    public void generateResponse(String prompt, String systemMessage) {
        // If prompt is empty, don't trigger anything - treat as normal text
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }

        if (mAIController.needModelClient()) {
            if (UiInteractor.getInstance().showChoseModelDialog()) {
                Context ctx = UiInteractor.getInstance().getContext();
                String msg = ctx != null ? ctx.getString(R.string.choose_model_message)
                        : "Chose and configure your language model";
                UiInteractor.getInstance().toastLong(msg);
            }
            return;
        }

        if (mAIController.needApiKey()) {
            if (UiInteractor.getInstance().showChoseModelDialog()) {
                Context ctx = UiInteractor.getInstance().getContext();
                String msg = ctx != null
                        ? ctx.getString(R.string.missing_api_key_message, mAIController.getLanguageModel().label)
                        : mAIController.getLanguageModel().label + " is Missing API Key";
                UiInteractor.getInstance().toastLong(msg);
            }
            return;
        }

        // Use thread pool instead of creating new threads
        aiExecutor.execute(() -> mAIController.generateResponse(prompt, systemMessage));
    }

    public void setTextActionMode(boolean enabled, String selectedText) {
        this.isTextActionMode = enabled;
        this.pendingSelectedText = selectedText;
    }

    public GenerativeAIController getController() {
        return mAIController;
    }

    // --- GenerativeAIListener Implementation ---

    @Override
    public void onAIPrepare() {
        if (onAiPrepareCallback != null) {
            onAiPrepareCallback.run();
        }

        // In text action mode, delete the selected text first
        if (isTextActionMode && pendingSelectedText != null) {
            // The selected text should already be selected, so we just need to delete it
            // and the AI response will replace it
            IMSController.getInstance().flush();
        } else {
            IMSController.getInstance().flush();
        }

        String generatingContent = getGeneratingContentString();
        IMSController.getInstance().commit(generatingContent);
        IMSController.getInstance().stopNotifyInput();
        IMSController.getInstance().startInputLock();
        justPrepared = true;
    }

    private void clearGeneratingContent() {
        if (justPrepared) {
            justPrepared = false;
            IMSController.getInstance().flush();
            String generatingContent = getGeneratingContentString();
            IMSController.getInstance().delete(generatingContent.length());
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
        Context ctx = UiInteractor.getInstance().getContext();
        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = ctx != null ? ctx.getString(R.string.unknown_error) : "Unknown error occurred";
        }

        String displayError = ctx != null ? ctx.getString(R.string.error_format, errorMsg)
                : "[Error: " + errorMsg + "]";
        IMSController.getInstance().flush();
        IMSController.getInstance().commit(displayError);
        IMSController.getInstance().startNotifyInput();

        // Reset text action mode
        setTextActionMode(false, null);
    }

    @Override
    public void onAIComplete() {
        IMSController.getInstance().endInputLock();
        clearGeneratingContent();
        IMSController.getInstance().startNotifyInput();

        // Reset text action mode
        setTextActionMode(false, null);
    }
}
