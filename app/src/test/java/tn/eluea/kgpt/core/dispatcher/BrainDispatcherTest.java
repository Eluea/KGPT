package tn.eluea.kgpt.core.dispatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tn.eluea.kgpt.core.ai.AiResponseManager;
import tn.eluea.kgpt.instruction.command.CommandManager;
import tn.eluea.kgpt.text.parse.result.AIParseResult;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;

public class BrainDispatcherTest {

    @Mock
    private AiResponseManager aiManager;
    @Mock
    private CommandManager commandManager;
    @Mock
    private UiInteractor uiInteractor;
    @Mock
    private IMSController imsController;

    private BrainDispatcher brainDispatcher;
    private MockedStatic<UiInteractor> mockedUiInteractor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock static UiInteractor
        mockedUiInteractor = Mockito.mockStatic(UiInteractor.class);
        mockedUiInteractor.when(UiInteractor::getInstance).thenReturn(uiInteractor);
        when(uiInteractor.getIMSController()).thenReturn(imsController);

        brainDispatcher = new BrainDispatcher(aiManager, commandManager);
    }

    @After
    public void tearDown() {
        mockedUiInteractor.close();
    }

    @Test
    public void dispatch_AIParseResult_DelegatesToAiManager() {
        String prompt = "Test Prompt";
        AIParseResult result = new AIParseResult(java.util.Arrays.asList("match", prompt), 0, 0);

        brainDispatcher.dispatch(result);

        verify(aiManager, times(1)).generateResponse(eq(prompt), any());
    }
}
