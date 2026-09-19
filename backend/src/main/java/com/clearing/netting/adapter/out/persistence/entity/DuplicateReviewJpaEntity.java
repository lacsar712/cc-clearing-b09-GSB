package com.clearing.netting.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "duplicate_group_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uk_duplicate_group_key",
        columnNames = {"settleDate", "currency", "payerMemberId", "payeeMemberId", "amount"}))
public class DuplicateReviewJpaEntity {

    @Id
    @Column(length = 64)
    private String reviewId;

    @Column(nullable = false)
    private LocalDate settleDate;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, length = 64)
    private String payerMemberId;

    @Column(nullable = false, length = 64)
    private String payeeMemberId;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant reviewedAt;

    @Column(nullable = false, length = 64)
    private String reviewedBy;

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public void setSettleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPayerMemberId() {
        return payerMemberId;
    }

    public void setPayerMemberId(String payerMemberId) {
        this.payerMemberId = payerMemberId;
    }

    public String getPayeeMemberId() {
        return payeeMemberId;
    }

    public void setPayeeMemberId(String payeeMemberId) {
        this.payeeMemberId = payeeMemberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
