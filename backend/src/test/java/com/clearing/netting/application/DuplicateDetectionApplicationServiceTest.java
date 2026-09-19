package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupResolution;
import com.clearing.netting.domain.model.DuplicateResolutionType;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.DuplicateResolutionRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateDetectionApplicationServiceTest {

    private static final LocalDate SETTLE_DATE = LocalDate.of(2026, 9, 19);

    private InMemoryObligationRepository obligationRepository;
    private InMemoryResolutionRepository resolutionRepository;
    private DuplicateDetectionApplicationService service;

    @BeforeEach
    void setUp() {
        obligationRepository = new InMemoryObligationRepository();
        resolutionRepository = new InMemoryResolutionRepository();
        service = new DuplicateDetectionApplicationService(obligationRepository, resolutionRepository);
    }

    @Test
    void groupsIdenticalOpenObligations() {
        TradeObligation o1 = open("A", "B", "100");
        TradeObligation o2 = open("A", "B", "100");
        open("A", "B", "200");           // different amount
        open("B", "A", "100");           // reversed direction
        open("A", "C", "100");           // different payee

        List<DuplicateGroup> groups = service.listGroups(SETTLE_DATE, "USD");

        assertEquals(1, groups.size());
        DuplicateGroup group = groups.get(0);
        assertTrue(group.pending());
        assertEquals(2, group.obligations().size());
        assertTrue(group.obligations().stream()
                .map(TradeObligation::getObligationId)
                .collect(Collectors.toSet())
                .containsAll(List.of(o1.getObligationId(), o2.getObligationId())));
        assertEquals(0, group.amount().compareTo(new BigDecimal("100.00000000")));
    }

    @Test
    void emptyWhenNoDuplicates() {
        open("A", "B", "100");
        open("A", "B", "101");
        assertTrue(service.listGroups(SETTLE_DATE, "USD").isEmpty());
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "USD"));
    }

    @Test
    void cancelRequiresReason() {
        TradeObligation o1 = open("A", "B", "100");
        open("A", "B", "100");
        String key = DuplicateDetectionApplicationService.keyOf(o1);

        DomainException ex = assertThrows(DomainException.class, () ->
                service.cancelDuplicate(key, o1.getObligationId(), "  ", "op"));
        assertEquals("REASON_REQUIRED", ex.getCode());
    }

    @Test
    void cancelRejectsMismatchedGroupKey() {
        TradeObligation o1 = open("A", "B", "100");
        open("A", "B", "100");

        DomainException ex = assertThrows(DomainException.class, () ->
                service.cancelDuplicate("2099-01-01|USD|A|B|100.00000000", o1.getObligationId(), "dup", "op"));
        assertEquals("GROUP_MISMATCH", ex.getCode());
    }

    @Test
    void cancellingOneOfPairClearsTheGroup() {
        TradeObligation o1 = open("A", "B", "100");
        TradeObligation o2 = open("A", "B", "100");
        String key = DuplicateDetectionApplicationService.keyOf(o1);
        assertTrue(service.hasPendingGroups(SETTLE_DATE, "USD"));

        service.cancelDuplicate(key, o1.getObligationId(), "重复录入", "op");

        assertEquals(ObligationStatus.CANCELLED, o1.getStatus());
        assertEquals("重复录入", o1.getCancelReason());
        assertEquals(ObligationStatus.OPEN, o2.getStatus());
        // only one OPEN left -> group no longer listed, netting unblocked
        assertTrue(service.listGroups(SETTLE_DATE, "USD").isEmpty());
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "USD"));
    }

    @Test
    void cancellingOneOfThreeMarksGroupResolved() {
        TradeObligation o1 = open("A", "B", "100");
        open("A", "B", "100");
        open("A", "B", "100");
        String key = DuplicateDetectionApplicationService.keyOf(o1);

        service.cancelDuplicate(key, o1.getObligationId(), "重复录入", "op");

        List<DuplicateGroup> groups = service.listGroups(SETTLE_DATE, "USD");
        assertEquals(1, groups.size());
        assertFalse(groups.get(0).pending());
        assertEquals(DuplicateResolutionType.CANCELLED, groups.get(0).resolution().getResolutionType());
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "USD"));
    }

    @Test
    void reviewedGroupStopsBlocking() {
        TradeObligation o1 = open("A", "B", "100");
        open("A", "B", "100");
        String key = DuplicateDetectionApplicationService.keyOf(o1);

        DuplicateGroup reviewed = service.reviewGroup(key, "两笔均为有效业务", "op");

        assertFalse(reviewed.pending());
        assertEquals(DuplicateResolutionType.REVIEWED, reviewed.resolution().getResolutionType());
        assertEquals("op", reviewed.resolution().getResolvedBy());
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "USD"));
    }

    @Test
    void newIdenticalOpenReflagsResolvedGroup() {
        TradeObligation o1 = open("A", "B", "100");
        open("A", "B", "100");
        String key = DuplicateDetectionApplicationService.keyOf(o1);
        service.reviewGroup(key, null, "op");
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "USD"));

        open("A", "B", "100"); // new collision after review

        assertTrue(service.hasPendingGroups(SETTLE_DATE, "USD"));
        List<DuplicateGroup> groups = service.listGroups(SETTLE_DATE, "USD");
        assertEquals(1, groups.size());
        assertTrue(groups.get(0).pending());
        assertEquals(3, groups.get(0).obligations().size());
    }

    @Test
    void reviewFailsWhenGroupDoesNotExist() {
        DomainException ex = assertThrows(DomainException.class, () ->
                service.reviewGroup("2026-09-19|USD|A|B|100.00000000", null, "op"));
        assertEquals("GROUP_NOT_FOUND", ex.getCode());
    }

    @Test
    void groupsAreScopedBySettleDateAndCurrency() {
        open("A", "B", "100");
        open("A", "B", "100");
        assertTrue(service.hasPendingGroups(SETTLE_DATE, "USD"));
        assertFalse(service.hasPendingGroups(SETTLE_DATE.plusDays(1), "USD"));
        assertFalse(service.hasPendingGroups(SETTLE_DATE, "CNY"));
    }

    private TradeObligation open(String payer, String payee, String amount) {
        TradeObligation o = TradeObligation.open(
                payer, payee, "USD", new BigDecimal(amount), SETTLE_DATE.minusDays(1), SETTLE_DATE);
        return obligationRepository.save(o);
    }

    private static final class InMemoryObligationRepository implements ObligationRepositoryPort {
        private final Map<String, TradeObligation> store = new LinkedHashMap<>();

        @Override
        public TradeObligation save(TradeObligation obligation) {
            store.put(obligation.getObligationId(), obligation);
            return obligation;
        }

        @Override
        public List<TradeObligation> saveAll(List<TradeObligation> obligations) {
            obligations.forEach(this::save);
            return obligations;
        }

        @Override
        public Optional<TradeObligation> findById(String obligationId) {
            return Optional.ofNullable(store.get(obligationId));
        }

        @Override
        public List<TradeObligation> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<TradeObligation> findByFilters(String currency, LocalDate settleDate, ObligationStatus status) {
            return store.values().stream()
                    .filter(o -> currency == null || o.getCurrency().equalsIgnoreCase(currency))
                    .filter(o -> settleDate == null || o.getSettleDate().equals(settleDate))
                    .filter(o -> status == null || o.getStatus() == status)
                    .collect(Collectors.toList());
        }

        @Override
        public List<TradeObligation> findOpenBySettleDateAndCurrency(LocalDate settleDate, String currency) {
            return findByFilters(currency, settleDate, ObligationStatus.OPEN);
        }

        @Override
        public List<TradeObligation> findByNettingRunId(String runId) {
            return store.values().stream()
                    .filter(o -> runId.equals(o.getNettingRunId()))
                    .collect(Collectors.toList());
        }
    }

    private static final class InMemoryResolutionRepository implements DuplicateResolutionRepositoryPort {
        private final Map<String, DuplicateGroupResolution> store = new LinkedHashMap<>();

        @Override
        public DuplicateGroupResolution save(DuplicateGroupResolution resolution) {
            store.put(resolution.getGroupKey(), resolution);
            return resolution;
        }

        @Override
        public List<DuplicateGroupResolution> findByGroupKeys(Collection<String> groupKeys) {
            return store.values().stream()
                    .filter(r -> groupKeys.contains(r.getGroupKey()))
                    .collect(Collectors.toList());
        }
    }
}
