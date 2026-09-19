package com.clearing.netting.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted review decision for a duplicate key. A group derived from current OPEN
 * obligations is REVIEWED only when every obligation in it was created at or before
 * {@code reviewedAt}; an identical obligation entered afterwards re-opens the group.
 */
public class DuplicateReview {
    private final String reviewId;
    private final DuplicateKey key;
    private Instant reviewedAt;
    private String reviewedBy;

    public DuplicateReview(String reviewId, DuplicateKey key, Instant reviewedAt, String reviewedBy) {
        this.reviewId = Objects.requireNonNull(reviewId);
        this.key = Objects.requireNonNull(key);
        this.reviewedAt = Objects.requireNonNull(reviewedAt);
        this.reviewedBy = Objects.requireNonNull(reviewedBy);
    }

    public static DuplicateReview create(DuplicateKey key, String reviewedBy) {
        return new DuplicateReview(UUID.randomUUID().toString(), key, Instant.now(), reviewedBy);
    }

    public void touch(String reviewedBy) {
        this.reviewedAt = Instant.now();
        this.reviewedBy = Objects.requireNonNull(reviewedBy);
    }

    public String getReviewId() {
        return reviewId;
    }

    public DuplicateKey getKey() {
        return key;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }
}
