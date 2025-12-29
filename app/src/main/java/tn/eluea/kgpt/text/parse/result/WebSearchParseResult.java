package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class WebSearchParseResult extends ParseResult {
    public final String query;

    protected WebSearchParseResult(List<String> groups, int indexStart, int indexEnd) {
        super(groups, indexStart, indexEnd);
        this.query = groups.get(1);
    }
}
