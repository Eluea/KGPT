package tn.eluea.kgpt.text.parse.result;

import java.util.List;

import tn.eluea.kgpt.text.transform.format.ConversionMethod;

public class FormatParseResult extends ParseResult {
    public final String target;

    public final ConversionMethod conversionMethod;

    protected FormatParseResult(List<String> groups, int indexStart, int indexEnd, ConversionMethod conversionMethod) {
        super(groups, indexStart, indexEnd);

        this.target = groups.get(1);
        this.conversionMethod = conversionMethod;
    }
}
