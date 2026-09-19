package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.DuplicateReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface DuplicateReviewJpaRepository extends JpaRepository<DuplicateReviewJpaEntity, String> {

    Optional<DuplicateReviewJpaEntity> findBySettleDateAndCurrencyAndPayerMemberIdAndPayeeMemberIdAndAmount(
            LocalDate settleDate, String currency, String payerMemberId, String payeeMemberId, BigDecimal amount);
}
