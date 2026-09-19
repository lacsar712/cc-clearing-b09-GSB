package com.clearing.netting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A group of OPEN obligations sharing the same settleDate, currency, payer, payee
 * and amount — suspected duplicates. Computed on the fly from OPEN obligations;
 * {@code resolution} is the latest handling record (null when never handled) and
 * {@code pending} tells whether the group still blocks netting.
 */
public record DuplicateGroup(
        String groupKey,
        LocalDate settleDate,
        String currency,
        String payerMemberId,
        String payeeMemberId,
        BigDecimal amount,
        List<TradeObligation> obligations,
        DuplicateGroupResolution resolution,
        boolean pending) {
}
