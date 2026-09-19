package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.DuplicateReviewJpaEntity;
import com.clearing.netting.adapter.out.persistence.repo.DuplicateReviewJpaRepository;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.DuplicateReview;
import com.clearing.netting.domain.port.out.DuplicateReviewRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DuplicateReviewRepositoryAdapter implements DuplicateReviewRepositoryPort {

    private final DuplicateReviewJpaRepository repository;

    public DuplicateReviewRepositoryAdapter(DuplicateReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DuplicateReview> findAll() {
        return repository.findAll().stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DuplicateReview> findByKey(DuplicateKey key) {
        return repository
                .findBySettleDateAndCurrencyAndPayerMemberIdAndPayeeMemberIdAndAmount(
                        key.settleDate(), key.currency(), key.payerMemberId(), key.payeeMemberId(), key.amount())
                .map(PersistenceMapper::toDomain);
    }

    @Override
    public DuplicateReview save(DuplicateReview review) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(review)));
    }
}
