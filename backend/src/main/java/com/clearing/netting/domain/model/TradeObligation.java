package com.clearing.netting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class TradeObligation {
    private final String obligationId;
    private final String payerMemberId;
    private final String payeeMemberId;
    private final String currency;
    private final BigDecimal amount;
    private final LocalDate tradeDate;
    private final LocalDate settleDate;
    private ObligationStatus status;
    private String nettingRunId;
    private final Instant createdAt;
    private String cancelReason;
    private Instant cancelledAt;

    public TradeObligation(
            String obligationId,
            String payerMemberId,
            String payeeMemberId,
            String currency,
            BigDecimal amount,
            LocalDate tradeDate,
            LocalDate settleDate,
            ObligationStatus status,
            String nettingRunId) {
        this(obligationId, payerMemberId, payeeMemberId, currency, amount, tradeDate, settleDate,
                status, nettingRunId, Instant.now(), null, null);
    }

    public TradeObligation(
            String obligationId,
            String payerMemberId,
            String payeeMemberId,
            String currency,
            BigDecimal amount,
            LocalDate tradeDate,
            LocalDate settleDate,
            ObligationStatus status,
            String nettingRunId,
            Instant createdAt,
            String cancelReason,
            Instant cancelledAt) {
        this.obligationId = Objects.requireNonNull(obligationId);
        this.payerMemberId = Objects.requireNonNull(payerMemberId);
        this.payeeMemberId = Objects.requireNonNull(payeeMemberId);
        this.currency = Objects.requireNonNull(currency).toUpperCase();
        this.amount = Objects.requireNonNull(amount).setScale(8, RoundingMode.HALF_UP);
        this.tradeDate = Objects.requireNonNull(tradeDate);
        this.settleDate = Objects.requireNonNull(settleDate);
        this.status = Objects.requireNonNull(status);
        this.nettingRunId = nettingRunId;
        // nullable: rows persisted before createdAt existed reconstitute with null
        this.createdAt = createdAt;
        this.cancelReason = cancelReason;
        this.cancelledAt = cancelledAt;
    }

    public static TradeObligation open(
            String payerMemberId,
            String payeeMemberId,
            String currency,
            BigDecimal amount,
            LocalDate tradeDate,
            LocalDate settleDate) {
        if (payerMemberId.equals(payeeMemberId)) {
            throw new IllegalArgumentException("payer and payee must differ");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new TradeObligation(
                UUID.randomUUID().toString(),
                payerMemberId,
                payeeMemberId,
                currency,
                amount,
                tradeDate,
                settleDate,
                ObligationStatus.OPEN,
                null,
                Instant.now(),
                null,
                null);
    }

    public void markNetted(String runId) {
        if (status != ObligationStatus.OPEN) {
            throw new IllegalStateException("only OPEN obligations can be netted");
        }
        this.status = ObligationStatus.NETTED;
        this.nettingRunId = runId;
    }

    public void markSettled() {
        if (status != ObligationStatus.NETTED) {
            throw new IllegalStateException("only NETTED obligations can be settled");
        }
        this.status = ObligationStatus.SETTLED;
    }

    public void cancel(String reason) {
        if (status != ObligationStatus.OPEN) {
            throw new IllegalStateException("only OPEN obligations can be cancelled");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("cancel reason is required");
        }
        this.status = ObligationStatus.CANCELLED;
        this.cancelReason = reason.trim();
        this.cancelledAt = Instant.now();
    }

    public String getObligationId() {
        return obligationId;
    }

    public String getPayerMemberId() {
        return payerMemberId;
    }

    public String getPayeeMemberId() {
        return payeeMemberId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public ObligationStatus getStatus() {
        return status;
    }

    public String getNettingRunId() {
        return nettingRunId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
