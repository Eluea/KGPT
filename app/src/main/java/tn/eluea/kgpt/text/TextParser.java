package tn.eluea.kgpt.text;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResult;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResultFactory;
import tn.eluea.kgpt.text.parse.result.ParseResultFactory;
import tn.eluea.kgpt.text.parse.ParseDirective;
import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.ui.UiInteractor;

public class TextParser implements ConfigChangeListener {
    private final List<ParseDirective> directives = new ArrayList<>();
    private String currentTriggerSymbol = "$";

    public TextParser() {
        UiInteractor.getInstance().registerConfigChangeListener(this);
        List<ParsePattern> parsePatterns = SPManager.getInstance().getParsePatterns();
        updatePatterns(parsePatterns);
    }

    private void updatePatterns(List<ParsePattern> parsePatterns) {
        directives.clear();
        for (ParsePattern parsePattern: parsePatterns) {
            directives.add(new ParseDirective(parsePattern.getPattern(),
                    ParseResultFactory.of(parsePattern.getType())));
            
            if (parsePattern.getType() == PatternType.CommandAI) {
                String symbol = PatternType.regexToSymbol(parsePattern.getPattern().pattern());
                if (symbol != null && !symbol.isEmpty()) {
                    currentTriggerSymbol = symbol;
                }
            }
        }
    }

    public ParseResult parse(String text, int cursor) {
        String textBeforeCursor = text.substring(0, cursor);
        
        InlineAskParseResult inlineAskResult = InlineAskParseResultFactory.parse(
            textBeforeCursor, currentTriggerSymbol);
        if (inlineAskResult != null) {
            return inlineAskResult;
        }

        for (ParseDirective directive: directives) {
            ParseResult parseResult = directive.parse(textBeforeCursor);
            if (parseResult != null) {
                return parseResult;
            }
        }

        return null;
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {}

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {}

    @Override
    public void onCommandsChange(String commandsRaw) {}

    @Override
    public void onPatternsChange(String patternsRaw) {
        updatePatterns(ParsePattern.decode(patternsRaw));
    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {}
}
