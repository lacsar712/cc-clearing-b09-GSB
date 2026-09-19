package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupStatus;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.TradeObligation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end acceptance flow: identical OPEN obligations form a suspected-duplicate
 * group that blocks netting until cancelled or marked reviewed.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dupflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DuplicateDetectionFlowTest {

    @Autowired
    private MemberApplicationService memberService;
    @Autowired
    private ObligationApplicationService obligationService;
    @Autowired
    private DuplicateDetectionApplicationService duplicateService;
    @Autowired
    private NettingApplicationService nettingService;

    @Test
    void cancellingDuplicateUnblocksNetting() {
        Member a = memberService.createMember("Flow Bank A");
        Member b = memberService.createMember("Flow Bank B");
        LocalDate settleDate = LocalDate.now().plusDays(30);

        TradeObligation o1 = createPair(a, b, "USD", "5000", settleDate);
        TradeObligation o2 = createPair(a, b, "USD", "5000", settleDate);

        // identical OPEN obligations are auto-grouped as PENDING
        List<DuplicateGroup> pending =
                duplicateService.listGroups(settleDate, "USD", DuplicateGroupStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals(2, pending.get(0).getObligations().size());

        // netting is blocked while the group is unhandled
        DomainException blocked = assertThrows(
                DomainException.class, () -> nettingService.execute(settleDate, "USD"));
        assertEquals("DUPLICATE_SUSPECTS_PENDING", blocked.getCode());

        // cancelling one leg dissolves the group and unblocks netting
        obligationService.cancel(o2.getObligationId(), "duplicate entry, keyed twice");
        assertTrue(duplicateService.listGroups(settleDate, "USD", null).isEmpty());

        NettingApplicationService.NettingRunResult result = nettingService.execute(settleDate, "USD");
        assertEquals(NettingRunStatus.COMPLETED, result.run().getStatus());
        assertEquals(1, result.obligations().size());
        assertEquals(o1.getObligationId(), result.obligations().get(0).getObligationId());
    }

    @Test
    void reviewUnblocksNettingAndNewEntryReappears() {
        Member a = memberService.createMember("Flow Bank C");
        Member b = memberService.createMember("Flow Bank D");
        LocalDate settleDate = LocalDate.now().plusDays(31);

        TradeObligation o1 = createPair(a, b, "EUR", "1200", settleDate);
        createPair(a, b, "EUR", "1200", settleDate);

        // marking the group reviewed unblocks netting without cancelling anything
        DuplicateGroup reviewed = duplicateService.review(DuplicateKey.of(o1), "tester");
        assertEquals(DuplicateGroupStatus.REVIEWED, reviewed.getStatus());
        assertTrue(duplicateService.listGroups(settleDate, "EUR", DuplicateGroupStatus.PENDING).isEmpty());

        // a newly entered identical obligation re-opens the group and blocks again
        createPair(a, b, "EUR", "1200", settleDate);
        List<DuplicateGroup> pending =
                duplicateService.listGroups(settleDate, "EUR", DuplicateGroupStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals(3, pending.get(0).getObligations().size());
        DomainException blocked = assertThrows(
                DomainException.class, () -> nettingService.execute(settleDate, "EUR"));
        assertEquals("DUPLICATE_SUSPECTS_PENDING", blocked.getCode());

        // reviewing the re-opened group lets netting complete
        duplicateService.review(DuplicateKey.of(o1), "tester");
        NettingApplicationService.NettingRunResult result = nettingService.execute(settleDate, "EUR");
        assertEquals(NettingRunStatus.COMPLETED, result.run().getStatus());
        assertEquals(3, result.obligations().size());
    }

    private TradeObligation createPair(Member a, Member b, String ccy, String amount, LocalDate settleDate) {
        return obligationService.create(
                a.getMemberId(),
                b.getMemberId(),
                ccy,
                new BigDecimal(amount),
                settleDate.minusDays(1),
                settleDate);
    }
}
