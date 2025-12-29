package tn.eluea.kgpt.text.transform.format;

public class ReplaceConversionMethod extends ConversionMethod {
    private final CharacterTable mTable;

    public ReplaceConversionMethod(CharacterTable table) {
        mTable = table;
    }

    @Override
    public String doConvert(char c) {
        return mTable.replace(c);
    }
}
