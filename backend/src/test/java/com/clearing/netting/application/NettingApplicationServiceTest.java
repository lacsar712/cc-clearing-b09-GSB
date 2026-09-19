package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NettingApplicationServiceTest {

    private static final LocalDate SETTLE_DATE = LocalDate.of(2026, 9, 19);

    private NettingRunRepositoryPort runRepository;
    private ObligationRepositoryPort obligationRepository;
    private MemberRepositoryPort memberRepository;
    private NetPositionRepositoryPort positionRepository;
    private NettingRunStatusService statusService;
    private DuplicateDetectionApplicationService duplicateDetectionService;
    private NettingApplicationService service;

    @BeforeEach
    void setUp() {
        runRepository = mock(NettingRunRepositoryPort.class);
        obligationRepository = mock(ObligationRepositoryPort.class);
        memberRepository = mock(MemberRepositoryPort.class);
        positionRepository = mock(NetPositionRepositoryPort.class);
        statusService = mock(NettingRunStatusService.class);
        duplicateDetectionService = mock(DuplicateDetectionApplicationService.class);
        service = new NettingApplicationService(
                runRepository,
                obligationRepository,
                memberRepository,
                positionRepository,
                statusService,
                duplicateDetectionService);

        lenient().when(statusService.saveInNewTx(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(obligationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(positionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeBlockedWhenPendingDuplicateGroupsExist() {
        when(duplicateDetectionService.hasPendingGroups(SETTLE_DATE, "USD")).thenReturn(true);

        DomainException ex = assertThrows(DomainException.class, () ->
                service.execute(SETTLE_DATE, "USD"));

        assertEquals("DUPLICATE_SUSPECTED", ex.getCode());
        verify(runRepository, never()).save(any());
        verify(statusService, never()).saveInNewTx(any());
        verify(obligationRepository, never()).findOpenBySettleDateAndCurrency(any(), anyString());
    }

    @Test
    void executeRunsWhenNoPendingDuplicateGroups() {
        when(duplicateDetectionService.hasPendingGroups(SETTLE_DATE, "USD")).thenReturn(false);
        TradeObligation o1 = TradeObligation.open("A", "B", "USD",
                new BigDecimal("100"), SETTLE_DATE.minusDays(1), SETTLE_DATE);
        TradeObligation o2 = TradeObligation.open("B", "A", "USD",
                new BigDecimal("40"), SETTLE_DATE.minusDays(1), SETTLE_DATE);
        when(obligationRepository.findOpenBySettleDateAndCurrency(SETTLE_DATE, "USD"))
                .thenReturn(List.of(o1, o2));
        when(memberRepository.findByIds(anySet())).thenReturn(List.of(
                new Member("A", "Bank A", MemberStatus.ACTIVE),
                new Member("B", "Bank B", MemberStatus.ACTIVE)));

        NettingApplicationService.NettingRunResult result = service.execute(SETTLE_DATE, "USD");

        NettingRun run = result.run();
        assertEquals(NettingRunStatus.COMPLETED, run.getStatus());
        assertEquals(2, result.positions().size());
        verify(duplicateDetectionService).hasPendingGroups(SETTLE_DATE, "USD");
        verify(runRepository).save(any());
    }
}
