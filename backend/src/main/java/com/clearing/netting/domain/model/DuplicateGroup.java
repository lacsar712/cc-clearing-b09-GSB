package com.clearing.netting.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Derived (non-persisted) view of a suspected-duplicate group: the OPEN obligations
 * sharing one {@link DuplicateKey}, plus the review state resolved against stored
 * {@link DuplicateReview} decisions.
 */
public class DuplicateGroup {
    private final DuplicateKey key;
    private final List<TradeObligation> obligations;
    private final DuplicateGroupStatus status;
    private final Instant reviewedAt;
    private final String reviewedBy;

    public DuplicateGroup(
            DuplicateKey key,
            List<TradeObligation> obligations,
            DuplicateGroupStatus status,
            Instant reviewedAt,
            String reviewedBy) {
        this.key = Objects.requireNonNull(key);
        this.obligations = List.copyOf(obligations);
        if (this.obligations.size() < 2) {
            throw new IllegalArgumentException("a duplicate group requires at least 2 obligations");
        }
        this.status = Objects.requireNonNull(status);
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
    }

    public String getGroupId() {
        return key.stableId();
    }

    public DuplicateKey getKey() {
        return key;
    }

    public List<TradeObligation> getObligations() {
        return obligations;
    }

    public DuplicateGroupStatus getStatus() {
        return status;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }
}
