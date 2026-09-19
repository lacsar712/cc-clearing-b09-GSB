package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.DuplicateGroupResolution;

import java.util.Collection;
import java.util.List;

public interface DuplicateResolutionRepositoryPort {
    DuplicateGroupResolution save(DuplicateGroupResolution resolution);

    List<DuplicateGroupResolution> findByGroupKeys(Collection<String> groupKeys);
}
