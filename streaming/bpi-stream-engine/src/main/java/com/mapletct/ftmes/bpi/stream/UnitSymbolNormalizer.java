package com.mapletct.ftmes.bpi.stream;

import java.text.Normalizer;
import java.util.Locale;

final class UnitSymbolNormalizer {
    private UnitSymbolNormalizer() {
    }

    static boolean equivalent(String left, String right) {
        if (left == null || right == null) return false;
        return canonical(left).equals(canonical(right));
    }

    private static String canonical(String value) {
        return Normalizer
                .normalize(value.trim(), Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }
}
