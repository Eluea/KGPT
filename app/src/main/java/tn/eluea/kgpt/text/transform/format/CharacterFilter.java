package tn.eluea.kgpt.text.transform.format;

public interface CharacterFilter {

    CharacterFilter noCharacterFilter = c -> true;

    boolean filterCharacter(char c);
}