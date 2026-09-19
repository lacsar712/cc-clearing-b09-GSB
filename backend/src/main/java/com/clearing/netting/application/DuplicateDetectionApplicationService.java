package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupResolution;
import com.clearing.netting.domain.model.DuplicateResolutionType;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.DuplicateResolutionRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detects suspected-duplicate OPEN obligations: identical settleDate, currency,
 * payer, payee and amount. Groups stay "pending" until handled (one obligation
 * cancelled with a reason, or the whole group marked reviewed); a handled group
 * turns pending again when new identical OPEN obligations arrive.
 */
@Service
public class DuplicateDetectionApplicationService {

    private final ObligationRepositoryPort obligationRepository;
    private final DuplicateResolutionRepositoryPort resolutionRepository;

    public DuplicateDetectionApplicationService(
            ObligationRepositoryPort obligationRepository,
            DuplicateResolutionRepositoryPort resolutionRepository) {
        this.obligationRepository = obligationRepository;
        this.resolutionRepository = resolutionRepository;
    }

    public static String keyOf(TradeObligation o) {
        return o.getSettleDate()
                + "|" + o.getCurrency()
                + "|" + o.getPayerMemberId()
                + "|" + o.getPayeeMemberId()
                + "|" + o.getAmount().toPlainString();
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroup> listGroups(LocalDate settleDate, String currency) {
        List<TradeObligation> opens = obligationRepository.findByFilters(currency, settleDate, ObligationStatus.OPEN);
        Map<String, List<TradeObligation>> grouped = new LinkedHashMap<>();
        for (TradeObligation o : opens) {
            grouped.computeIfAbsent(keyOf(o), k -> new ArrayList<>()).add(o);
        }
        grouped.entrySet().removeIf(e -> e.getValue().size() < 2);
        if (grouped.isEmpty()) {
            return List.of();
        }
        Map<String, DuplicateGroupResolution> resolutions = resolutionRepository
                .findByGroupKeys(grouped.keySet()).stream()
                .collect(Collectors.toMap(DuplicateGroupResolution::getGroupKey, Function.identity()));
        List<DuplicateGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<TradeObligation>> e : grouped.entrySet()) {
            groups.add(buildGroup(e.getKey(), e.getValue(), resolutions.get(e.getKey())));
        }
        groups.sort(Comparator.comparing(DuplicateGroup::pending).reversed()
                .thenComparing(DuplicateGroup::settleDate, Comparator.reverseOrder())
                .thenComparing(DuplicateGroup::groupKey));
        return groups;
    }

    @Transactional(readOnly = true)
    public boolean hasPendingGroups(LocalDate settleDate, String currency) {
        return listGroups(settleDate, currency).stream().anyMatch(DuplicateGroup::pending);
    }

    @Transactional
    public DuplicateGroup cancelDuplicate(String groupKey, String obligationId, String reason, String operator) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("REASON_REQUIRED", "cancel reason is required");
        }
        TradeObligation obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new DomainException("OBLIGATION_NOT_FOUND", "obligation not found: " + obligationId));
        if (!keyOf(obligation).equals(groupKey)) {
            throw new DomainException("GROUP_MISMATCH", "obligation does not belong to group: " + groupKey);
        }
        if (obligation.getStatus() != ObligationStatus.OPEN) {
            throw new DomainException("INVALID_STATE", "only OPEN obligations can be cancelled: " + obligationId);
        }
        obligation.markCancelled(reason.trim());
        obligationRepository.save(obligation);

        List<TradeObligation> remaining = openGroupMembers(groupKey);
        DuplicateGroupResolution resolution = resolutionRepository.save(DuplicateGroupResolution.of(
                groupKey, remaining.size(), DuplicateResolutionType.CANCELLED, reason.trim(), operator));
        return buildGroup(groupKey, remaining, resolution);
    }

    @Transactional
    public DuplicateGroup reviewGroup(String groupKey, String note, String operator) {
        List<TradeObligation> members = openGroupMembers(groupKey);
        if (members.size() < 2) {
            throw new DomainException("GROUP_NOT_FOUND", "no pending duplicate group for key: " + groupKey);
        }
        DuplicateGroupResolution resolution = resolutionRepository.save(DuplicateGroupResolution.of(
                groupKey, members.size(), DuplicateResolutionType.REVIEWED,
                note == null || note.isBlank() ? null : note.trim(), operator));
        return buildGroup(groupKey, members, resolution);
    }

    private List<TradeObligation> openGroupMembers(String groupKey) {
        String[] parts = groupKey.split("\\|", -1);
        if (parts.length != 5) {
            throw new DomainException("INVALID_GROUP_KEY", "malformed group key: " + groupKey);
        }
        LocalDate settleDate;
        try {
            settleDate = LocalDate.parse(parts[0]);
        } catch (RuntimeException ex) {
            throw new DomainException("INVALID_GROUP_KEY", "malformed group key: " + groupKey);
        }
        return obligationRepository.findByFilters(parts[1], settleDate, ObligationStatus.OPEN).stream()
                .filter(o -> keyOf(o).equals(groupKey))
                .collect(Collectors.toList());
    }

    private DuplicateGroup buildGroup(
            String groupKey, List<TradeObligation> members, DuplicateGroupResolution resolution) {
        List<TradeObligation> sorted = members.stream()
                .sorted(Comparator.comparing(TradeObligation::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        boolean pending = sorted.size() >= 2
                && (resolution == null || sorted.size() > resolution.getOpenCount());
        TradeObligation first = sorted.isEmpty() ? null : sorted.get(0);
        return new DuplicateGroup(
                groupKey,
                first == null ? null : first.getSettleDate(),
                first == null ? null : first.getCurrency(),
                first == null ? null : first.getPayerMemberId(),
                first == null ? null : first.getPayeeMemberId(),
                first == null ? null : first.getAmount(),
                sorted,
                resolution,
                pending);
    }
}
