package com.clearing.netting.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Records that a suspected-duplicate group (identified by its groupKey) was handled:
 * either one obligation was cancelled, or the whole group was reviewed as harmless.
 * openCount captures how many OPEN obligations the group had right after handling;
 * if more identical OPEN obligations appear later (openCount grows), the group
 * becomes pending again.
 */
public class DuplicateGroupResolution {
    private final String groupKey;
    private final int openCount;
    private final DuplicateResolutionType resolutionType;
    private final String reason;
    private final String resolvedBy;
    private final Instant resolvedAt;

    public DuplicateGroupResolution(
            String groupKey,
            int openCount,
            DuplicateResolutionType resolutionType,
            String reason,
            String resolvedBy,
            Instant resolvedAt) {
        this.groupKey = Objects.requireNonNull(groupKey);
        this.openCount = openCount;
        this.resolutionType = Objects.requireNonNull(resolutionType);
        this.reason = reason;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Objects.requireNonNull(resolvedAt);
    }

    public static DuplicateGroupResolution of(
            String groupKey,
            int openCount,
            DuplicateResolutionType resolutionType,
            String reason,
            String resolvedBy) {
        return new DuplicateGroupResolution(
                groupKey, openCount, resolutionType, reason, resolvedBy, Instant.now());
    }

    public String getGroupKey() {
        return groupKey;
    }

    public int getOpenCount() {
        return openCount;
    }

    public DuplicateResolutionType getResolutionType() {
        return resolutionType;
    }

    public String getReason() {
        return reason;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
