package tn.eluea.kgpt.core.di;

import android.content.Context;
import android.util.Log;

import tn.eluea.kgpt.SPUpdater;
import tn.eluea.kgpt.core.ai.AiResponseManager;
import tn.eluea.kgpt.core.dispatcher.BrainDispatcher;
import tn.eluea.kgpt.features.textactions.data.TextActionManager;
import tn.eluea.kgpt.instruction.command.CommandManager;
import tn.eluea.kgpt.llm.GenerativeAIController;
import tn.eluea.kgpt.text.TextParser;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager;

/**
 * A simple Service Locator to manage dependencies and decouple instantiation.
 * This acts as a poor man's Dependency Injection container.
 * 
 * Note: This class is safe to use in both Xposed and non-Xposed contexts.
 */
public class ServiceLocator {
    
    private static final String TAG = "KGPT_ServiceLocator";

    private static ServiceLocator instance;
    
    // Flag to check if we're in Xposed context
    private static final boolean IS_XPOSED_CONTEXT;
    static {
        boolean xposedAvailable = false;
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            xposedAvailable = true;
        } catch (ClassNotFoundException e) {
            xposedAvailable = false;
        }
        IS_XPOSED_CONTEXT = xposedAvailable;
    }

    // Singleton dependencies
    private GenerativeAIController generativeAIController;
    private AiResponseManager aiResponseManager;
    private CommandManager commandManager;
    private BrainDispatcher brainDispatcher;
    private TextParser textParser;
    private SPUpdater spUpdater;

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }
    
    /**
     * Check if running in Xposed context.
     */
    public static boolean isXposedContext() {
        return IS_XPOSED_CONTEXT;
    }

    private ServiceLocator() {
        try {
            // Initialize singletons that don't need Context immediately
            generativeAIController = new GenerativeAIController();
            commandManager = new CommandManager();
            spUpdater = new SPUpdater();

            // AiResponseManager depends on GenerativeAIController
            aiResponseManager = new AiResponseManager(generativeAIController, null);

            // BrainDispatcher depends on AiResponseManager and CommandManager
            brainDispatcher = new BrainDispatcher(aiResponseManager, commandManager);

            textParser = new TextParser();
            
            Log.d(TAG, "ServiceLocator initialized successfully. Xposed context: " + IS_XPOSED_CONTEXT);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ServiceLocator", e);
            throw e;
        }
    }

    public GenerativeAIController getGenerativeAIController() {
        return generativeAIController;
    }

    public AiResponseManager getAiResponseManager() {
        return aiResponseManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public BrainDispatcher getBrainDispatcher() {
        return brainDispatcher;
    }

    public TextParser getTextParser() {
        return textParser;
    }

    public SPUpdater getSpUpdater() {
        return spUpdater;
    }

    // Factory methods for dependencies needing Context
    public AppTriggerManager createAppTriggerManager(Context context) {
        return new AppTriggerManager(context);
    }

    public TextActionManager createTextActionManager(Context context) {
        return new TextActionManager(context);
    }
}
