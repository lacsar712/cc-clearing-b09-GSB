package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.DuplicateResolutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DuplicateResolutionJpaRepository extends JpaRepository<DuplicateResolutionJpaEntity, String> {

    List<DuplicateResolutionJpaEntity> findByGroupKeyIn(Collection<String> groupKeys);
}
