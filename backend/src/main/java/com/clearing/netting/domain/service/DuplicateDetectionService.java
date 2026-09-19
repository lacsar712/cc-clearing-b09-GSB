package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupStatus;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.DuplicateReview;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Groups OPEN obligations sharing the same {@link DuplicateKey} (settle date, currency,
 * payer, payee, amount) into suspected-duplicate groups and resolves each group's state
 * against persisted review decisions.
 */
public class DuplicateDetectionService {

    public List<DuplicateGroup> detect(List<TradeObligation> obligations, List<DuplicateReview> reviews) {
        Map<DuplicateKey, DuplicateReview> reviewByKey = reviews.stream()
                .collect(Collectors.toMap(DuplicateReview::getKey, Function.identity(), (a, b) -> a));

        Map<DuplicateKey, List<TradeObligation>> byKey = new LinkedHashMap<>();
        for (TradeObligation o : obligations) {
            if (o.getStatus() != ObligationStatus.OPEN) {
                continue;
            }
            byKey.computeIfAbsent(DuplicateKey.of(o), k -> new ArrayList<>()).add(o);
        }

        List<DuplicateGroup> groups = new ArrayList<>();
        for (Map.Entry<DuplicateKey, List<TradeObligation>> entry : byKey.entrySet()) {
            List<TradeObligation> members = entry.getValue();
            if (members.size() < 2) {
                continue;
            }
            members.sort(Comparator.comparing(TradeObligation::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TradeObligation::getObligationId));
            DuplicateReview review = reviewByKey.get(entry.getKey());
            boolean reviewed = review != null && members.stream()
                    .allMatch(o -> createdBeforeReview(o, review.getReviewedAt()));
            groups.add(new DuplicateGroup(
                    entry.getKey(),
                    members,
                    reviewed ? DuplicateGroupStatus.REVIEWED : DuplicateGroupStatus.PENDING,
                    review == null ? null : review.getReviewedAt(),
                    review == null ? null : review.getReviewedBy()));
        }

        groups.sort(Comparator
                .comparing((DuplicateGroup g) -> g.getStatus() == DuplicateGroupStatus.PENDING ? 0 : 1)
                .thenComparing(g -> g.getKey().settleDate(), Comparator.reverseOrder())
                .thenComparing(DuplicateGroup::getGroupId));
        return groups;
    }

    /**
     * An obligation counts as covered by a review only if it existed at review time.
     * Legacy rows without createdAt are treated as old; anything created strictly
     * after the review re-opens the group (new suspected duplicate).
     */
    private boolean createdBeforeReview(TradeObligation o, Instant reviewedAt) {
        return o.getCreatedAt() == null || !o.getCreatedAt().isAfter(reviewedAt);
    }
}
