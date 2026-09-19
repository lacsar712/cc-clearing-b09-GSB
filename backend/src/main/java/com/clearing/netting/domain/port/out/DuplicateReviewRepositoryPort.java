package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.DuplicateReview;

import java.util.List;
import java.util.Optional;

public interface DuplicateReviewRepositoryPort {
    List<DuplicateReview> findAll();

    Optional<DuplicateReview> findByKey(DuplicateKey key);

    DuplicateReview save(DuplicateReview review);
}
