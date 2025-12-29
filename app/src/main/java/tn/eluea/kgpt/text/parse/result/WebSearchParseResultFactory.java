package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class WebSearchParseResultFactory implements ParseResultFactory {
    public WebSearchParseResultFactory() {
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        return new WebSearchParseResult(groups, indexStart, indexEnd);
    }
}
