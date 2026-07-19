package com.mapletct.ftmes.bpi.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitSymbolNormalizerTest {

    @Test
    void acceptsAsciiAndJetLinksSuperscriptFlowUnitsAsEquivalent() {
        assertTrue(UnitSymbolNormalizer.equivalent("m3/h", "m\u00b3/h"));
        assertTrue(UnitSymbolNormalizer.equivalent(" m\u00b3 / h ", "m3/h"));
    }

    @Test
    void keepsDifferentDimensionsAndRatesDistinct() {
        assertFalse(UnitSymbolNormalizer.equivalent("m3/h", "m3/s"));
        assertFalse(UnitSymbolNormalizer.equivalent("m3/h", "m/h"));
        assertFalse(UnitSymbolNormalizer.equivalent(null, "m3/h"));
    }
}
