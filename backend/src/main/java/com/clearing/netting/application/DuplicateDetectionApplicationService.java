package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupStatus;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.DuplicateReview;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.DuplicateReviewRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import com.clearing.netting.domain.service.DuplicateDetectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DuplicateDetectionApplicationService {

    private final ObligationRepositoryPort obligationRepository;
    private final DuplicateReviewRepositoryPort reviewRepository;
    private final DuplicateDetectionService detectionService;

    public DuplicateDetectionApplicationService(
            ObligationRepositoryPort obligationRepository,
            DuplicateReviewRepositoryPort reviewRepository) {
        this.obligationRepository = obligationRepository;
        this.reviewRepository = reviewRepository;
        this.detectionService = new DuplicateDetectionService();
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroup> listGroups(LocalDate settleDate, String currency, DuplicateGroupStatus status) {
        List<TradeObligation> opens =
                obligationRepository.findByFilters(currency, settleDate, ObligationStatus.OPEN);
        List<DuplicateGroup> groups = detectionService.detect(opens, reviewRepository.findAll());
        if (status == null) {
            return groups;
        }
        return groups.stream().filter(g -> g.getStatus() == status).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPendingGroups(LocalDate settleDate, String currency) {
        return !listGroups(settleDate, currency, DuplicateGroupStatus.PENDING).isEmpty();
    }

    @Transactional
    public DuplicateGroup review(DuplicateKey key, String reviewedBy) {
        List<TradeObligation> opens =
                obligationRepository.findByFilters(key.currency(), key.settleDate(), ObligationStatus.OPEN);
        long matching = opens.stream().filter(o -> DuplicateKey.of(o).equals(key)).count();
        if (matching < 2) {
            throw new DomainException("GROUP_NOT_FOUND", "no active duplicate group for the given key");
        }

        DuplicateReview review = reviewRepository.findByKey(key)
                .map(existing -> {
                    existing.touch(reviewedBy);
                    return existing;
                })
                .orElseGet(() -> DuplicateReview.create(key, reviewedBy));
        reviewRepository.save(review);

        return detectionService.detect(opens, reviewRepository.findAll()).stream()
                .filter(g -> g.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new DomainException("GROUP_NOT_FOUND", "no active duplicate group for the given key"));
    }
}
