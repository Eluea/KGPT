package tn.eluea.kgpt.text.parse.result;

import java.util.List;

import tn.eluea.kgpt.text.transform.format.ConversionMethod;

public class AIParseResultFactory implements ParseResultFactory {
    public AIParseResultFactory() {
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        return new AIParseResult(groups, indexStart, indexEnd);
    }
}
