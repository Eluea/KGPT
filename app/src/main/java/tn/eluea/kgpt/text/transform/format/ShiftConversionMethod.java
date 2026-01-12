/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * This file is part of KGPT.
 * Based on original code from KeyboardGPT by Mino260806.
 * Original: https://github.com/Mino260806/KeyboardGPT
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.text.transform.format;

public class ShiftConversionMethod extends ConversionMethod {
    private int mOffset;

    public ShiftConversionMethod(int offset) {
        mOffset = offset;
    }

    @Override
    public String doConvert(char c) {
        if (!ConversionMethod.isAscii(c))
            return String.valueOf(c);
        int offset = mOffset;
        if (Character.isUpperCase(c)) {
            offset -= 0x1a;
            c = Character.toLowerCase(c);
        }
        return new String(Character.toChars((c - 'a') + offset));
    }
}
