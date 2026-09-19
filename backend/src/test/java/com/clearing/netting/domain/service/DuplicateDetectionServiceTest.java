package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupStatus;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.DuplicateReview;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateDetectionServiceTest {

    private DuplicateDetectionService service;
    private LocalDate settleDate;
    private Instant now;

    @BeforeEach
    void setUp() {
        service = new DuplicateDetectionService();
        settleDate = LocalDate.of(2026, 9, 10);
        now = Instant.now();
    }

    @Test
    void groupsIdenticalOpenObligations() {
        List<TradeObligation> obligations = List.of(
                obligation("A", "B", "100", now.minusSeconds(600)),
                obligation("A", "B", "100", now.minusSeconds(300)),
                obligation("A", "B", "200", now.minusSeconds(100)));

        List<DuplicateGroup> groups = service.detect(obligations, List.of());

        assertEquals(1, groups.size());
        DuplicateGroup group = groups.get(0);
        assertEquals(DuplicateGroupStatus.PENDING, group.getStatus());
        assertEquals(2, group.getObligations().size());
        assertEquals(0, group.getKey().amount().compareTo(new BigDecimal("100")));
    }

    @Test
    void ignoresSingletonsAndNonOpenObligations() {
        List<TradeObligation> obligations = List.of(
                obligation("A", "B", "100", now.minusSeconds(600)),
                obligation("A", "B", "100", now.minusSeconds(300), ObligationStatus.CANCELLED),
                obligation("B", "A", "100", now.minusSeconds(100)));

        assertTrue(service.detect(obligations, List.of()).isEmpty());
    }

    @Test
    void reviewCoversGroupWhenAllObligationsPrecedeIt() {
        TradeObligation o1 = obligation("A", "B", "100", now.minusSeconds(600));
        TradeObligation o2 = obligation("A", "B", "100", now.minusSeconds(300));
        DuplicateReview review = new DuplicateReview(
                UUID.randomUUID().toString(), DuplicateKey.of(o1), now, "operator");

        List<DuplicateGroup> groups = service.detect(List.of(o1, o2), List.of(review));

        assertEquals(1, groups.size());
        assertEquals(DuplicateGroupStatus.REVIEWED, groups.get(0).getStatus());
        assertEquals("operator", groups.get(0).getReviewedBy());
    }

    @Test
    void newObligationAfterReviewReopensGroup() {
        TradeObligation o1 = obligation("A", "B", "100", now.minusSeconds(600));
        TradeObligation o2 = obligation("A", "B", "100", now.minusSeconds(300));
        TradeObligation late = obligation("A", "B", "100", now);
        DuplicateReview review = new DuplicateReview(
                UUID.randomUUID().toString(), DuplicateKey.of(o1), now.minusSeconds(100), "operator");

        List<DuplicateGroup> groups = service.detect(List.of(o1, o2, late), List.of(review));

        assertEquals(1, groups.size());
        assertEquals(DuplicateGroupStatus.PENDING, groups.get(0).getStatus());
        assertEquals(3, groups.get(0).getObligations().size());
    }

    @Test
    void legacyRowsWithoutCreatedAtStayReviewed() {
        TradeObligation o1 = obligation("A", "B", "100", null);
        TradeObligation o2 = obligation("A", "B", "100", null);
        DuplicateReview review = new DuplicateReview(
                UUID.randomUUID().toString(), DuplicateKey.of(o1), now, "operator");

        List<DuplicateGroup> groups = service.detect(List.of(o1, o2), List.of(review));

        assertEquals(DuplicateGroupStatus.REVIEWED, groups.get(0).getStatus());
    }

    private TradeObligation obligation(String payer, String payee, String amount, Instant createdAt) {
        return obligation(payer, payee, amount, createdAt, ObligationStatus.OPEN);
    }

    private TradeObligation obligation(
            String payer, String payee, String amount, Instant createdAt, ObligationStatus status) {
        return new TradeObligation(
                UUID.randomUUID().toString(),
                payer,
                payee,
                "USD",
                new BigDecimal(amount),
                settleDate.minusDays(1),
                settleDate,
                status,
                null,
                createdAt,
                null,
                null);
    }
}
