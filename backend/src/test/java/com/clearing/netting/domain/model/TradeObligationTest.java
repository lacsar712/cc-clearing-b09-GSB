package com.clearing.netting.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeObligationTest {

    @Test
    void cancelSetsStatusReasonAndTimestamp() {
        TradeObligation o = open();

        o.cancel("  duplicate entry, keyed twice  ");

        assertEquals(ObligationStatus.CANCELLED, o.getStatus());
        assertEquals("duplicate entry, keyed twice", o.getCancelReason());
        assertNotNull(o.getCancelledAt());
    }

    @Test
    void cancelRequiresReason() {
        TradeObligation o = open();
        assertThrows(IllegalArgumentException.class, () -> o.cancel(" "));
        assertEquals(ObligationStatus.OPEN, o.getStatus());
    }

    @Test
    void onlyOpenObligationsCanBeCancelled() {
        TradeObligation o = open();
        o.markNetted("run-1");
        assertThrows(IllegalStateException.class, () -> o.cancel("too late"));
    }

    private TradeObligation open() {
        return TradeObligation.open(
                "A",
                "B",
                "USD",
                new BigDecimal("100"),
                LocalDate.of(2026, 9, 9),
                LocalDate.of(2026, 9, 10));
    }
}
