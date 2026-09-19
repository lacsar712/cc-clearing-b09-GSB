package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.DuplicateResolutionJpaRepository;
import com.clearing.netting.domain.model.DuplicateGroupResolution;
import com.clearing.netting.domain.port.out.DuplicateResolutionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DuplicateResolutionRepositoryAdapter implements DuplicateResolutionRepositoryPort {

    private final DuplicateResolutionJpaRepository repository;

    public DuplicateResolutionRepositoryAdapter(DuplicateResolutionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DuplicateGroupResolution save(DuplicateGroupResolution resolution) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(resolution)));
    }

    @Override
    public List<DuplicateGroupResolution> findByGroupKeys(Collection<String> groupKeys) {
        if (groupKeys == null || groupKeys.isEmpty()) {
            return List.of();
        }
        return repository.findByGroupKeyIn(groupKeys).stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
