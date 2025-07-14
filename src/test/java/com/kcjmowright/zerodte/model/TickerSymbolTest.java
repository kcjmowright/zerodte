package com.kcjmowright.zerodte.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TickerSymbolTest {

    @Test
    void shouldParseCallTickerSymbol() {
        String tickerSymbol = "XYZ 210115C00050000";
        TickerSymbol actual = TickerSymbol.fromString(tickerSymbol);
        TickerSymbol expected = new TickerSymbol();
        expected.setSymbol(tickerSymbol);
        expected.setUnderlyingSymbol("XYZ");
        expected.setType(InstrumentType.CALL);
        expected.setExpirationDate(LocalDateTime.of(2021, 1, 15, 0, 0));
        expected.setStrikePrice(new BigDecimal("50.000").setScale(3, RoundingMode.HALF_UP));
        assertEquals(expected, actual);
    }

    @Test
    void shouldParsePutTickerSymbol() {
        String tickerSymbol = "QQQ 210215P00050250";
        TickerSymbol actual = TickerSymbol.fromString(tickerSymbol);
        TickerSymbol expected = new TickerSymbol();
        expected.setSymbol(tickerSymbol);
        expected.setUnderlyingSymbol("QQQ");
        expected.setType(InstrumentType.PUT);
        expected.setExpirationDate(LocalDateTime.of(2021, 2, 15, 0, 0));
        expected.setStrikePrice(new BigDecimal("50.250").setScale(3, RoundingMode.HALF_UP));
        assertEquals(expected, actual);
    }
}
