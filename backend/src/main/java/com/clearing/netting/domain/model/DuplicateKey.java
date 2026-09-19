package com.clearing.netting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a suspected-duplicate group: OPEN obligations sharing the same
 * settle date, currency, payer, payee and amount are considered suspected duplicates.
 */
public record DuplicateKey(
        LocalDate settleDate,
        String currency,
        String payerMemberId,
        String payeeMemberId,
        BigDecimal amount) {

    public DuplicateKey {
        Objects.requireNonNull(settleDate, "settleDate");
        currency = Objects.requireNonNull(currency, "currency").trim().toUpperCase();
        payerMemberId = Objects.requireNonNull(payerMemberId, "payerMemberId");
        payeeMemberId = Objects.requireNonNull(payeeMemberId, "payeeMemberId");
        amount = Objects.requireNonNull(amount, "amount").setScale(8, RoundingMode.HALF_UP);
    }

    public static DuplicateKey of(TradeObligation o) {
        return new DuplicateKey(
                o.getSettleDate(),
                o.getCurrency(),
                o.getPayerMemberId(),
                o.getPayeeMemberId(),
                o.getAmount());
    }

    /** Deterministic, stable identifier for display and client-side keying. */
    public String stableId() {
        String raw = settleDate + "|" + currency + "|" + payerMemberId + "|" + payeeMemberId
                + "|" + amount.toPlainString();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
