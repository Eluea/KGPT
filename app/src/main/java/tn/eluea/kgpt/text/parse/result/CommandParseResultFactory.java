package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class CommandParseResultFactory implements ParseResultFactory {
    public CommandParseResultFactory() {
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        return new CommandParseResult(groups, indexStart, indexEnd);
    }
}
